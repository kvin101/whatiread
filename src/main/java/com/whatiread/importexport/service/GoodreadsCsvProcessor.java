package com.whatiread.importexport.service;

import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.importexport.util.SimpleCsv;
import com.whatiread.library.api.UpdateUserBookRequest;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shelf.api.AddShelfBookRequest;
import com.whatiread.shelf.api.CreateShelfRequest;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.service.ShelfService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GoodreadsCsvProcessor {

    private static final Set<String> SYSTEM_SHELVES = Set.of(
            "read", "currently-reading", "to-read", "did-not-finish"
    );

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private final UserLookupService userLookupService;
    private final ImportBookResolver importBookResolver;
    private final LibraryService libraryService;
    private final ShelfService shelfService;
    private final ImportJobStatusService importJobStatusService;
    private final BusinessMetrics businessMetrics;

    public GoodreadsCsvProcessor(
            UserLookupService userLookupService,
            ImportBookResolver importBookResolver,
            LibraryService libraryService,
            ShelfService shelfService,
            ImportJobStatusService importJobStatusService,
            BusinessMetrics businessMetrics
    ) {
        this.userLookupService = userLookupService;
        this.importBookResolver = importBookResolver;
        this.libraryService = libraryService;
        this.shelfService = shelfService;
        this.importJobStatusService = importJobStatusService;
        this.businessMetrics = businessMetrics;
    }

    private static String dedupeKey(String isbn, String title, List<String> authors) {
        if (StringUtils.hasText(isbn)) {
            return "isbn:" + isbn;
        }
        return "title:" + title.trim().toLowerCase(Locale.ROOT) + "|" + String.join(",", authors);
    }

    private static ReadingStatus mapExclusiveShelf(String exclusive) {
        if (!StringUtils.hasText(exclusive)) {
            return ReadingStatus.TO_READ;
        }
        return switch (exclusive.trim().toLowerCase(Locale.ROOT)) {
            case "read" -> ReadingStatus.READ;
            case "currently-reading" -> ReadingStatus.READING;
            case "did-not-finish" -> ReadingStatus.DNF;
            default -> ReadingStatus.TO_READ;
        };
    }

    private static Set<String> parseShelfNames(String bookshelves) {
        if (!StringUtils.hasText(bookshelves)) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        for (String part : bookshelves.split(",")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            String key = name.toLowerCase(Locale.ROOT);
            if (!SYSTEM_SHELVES.contains(key)) {
                names.add(name);
            }
        }
        return names;
    }

    private static List<String> parseAuthors(String authors) {
        if (!StringUtils.hasText(authors)) {
            return List.of("Unknown");
        }
        return Arrays.stream(authors.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private static String normalizeIsbn(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.replaceAll("[^0-9Xx]", "");
    }

    private static Integer parseInt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    @Async("importTaskExecutor")
    public void processJob(UUID jobId, UUID userId, byte[] csvBytes) {
        importJobStatusService.markProcessing(jobId);
        businessMetrics.recordImportStarted();
        try {
            GoodreadsImportResultDto result = importCsv(userId, new ByteArrayInputStream(csvBytes));
            importJobStatusService.markCompleted(jobId, result);
            businessMetrics.recordImportCompleted();
            businessMetrics.recordBooksImported(result.booksImported());
        } catch (Exception ex) {
            importJobStatusService.markFailed(jobId, ex.getMessage());
            businessMetrics.recordImportFailed();
        }
    }

    public GoodreadsImportResultDto importCsv(UUID userId, InputStream csvStream) {
        userLookupService.requireExists(userId);
        String content;
        try {
            content = new String(csvStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read CSV file");
        }
        List<Map<String, String>> rows = SimpleCsv.parse(content);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty or missing a header row");
        }

        Map<String, UUID> shelfCache = new HashMap<>();
        Set<String> seenRowKeys = new HashSet<>();
        int booksImported = 0;
        int shelvesCreated = 0;
        int skipped = 0;
        int duplicatesSkipped = 0;
        int errors = 0;

        for (Map<String, String> row : rows) {
            try {
                String title = SimpleCsv.get(row, "Title");
                if (!StringUtils.hasText(title)) {
                    skipped++;
                    continue;
                }
                String isbn13 = normalizeIsbn(SimpleCsv.get(row, "ISBN13", "ISBN"));
                List<String> authorList = parseAuthors(SimpleCsv.get(row, "Author", "Author l-f"));
                String rowKey = dedupeKey(isbn13, title, authorList);
                if (!seenRowKeys.add(rowKey)) {
                    duplicatesSkipped++;
                    continue;
                }

                UUID bookId = resolveBookId(row, title, authorList, isbn13);
                ReadingStatus status = mapExclusiveShelf(SimpleCsv.get(row, "Exclusive Shelf"));
                UserBookDto userBook = libraryService.upsertForImport(userId, bookId, status);
                applyRating(userId, userBook, SimpleCsv.get(row, "My Rating"));
                applyFinishedDate(userId, userBook, SimpleCsv.get(row, "Date Read"));

                for (String shelfName : parseShelfNames(SimpleCsv.get(row, "Bookshelves"))) {
                    String shelfKey = shelfName.toLowerCase(Locale.ROOT).trim();
                    boolean[] createdFlag = {false};
                    UUID shelfId = shelfCache.computeIfAbsent(
                            shelfKey, key -> {
                                ShelfRef ref = ensureShelf(userId, shelfName);
                                createdFlag[0] = ref.created();
                                return ref.id();
                            });
                    if (createdFlag[0]) {
                        shelvesCreated++;
                    }
                    if (!shelfService.hasUserBookOnShelf(shelfId, userBook.id())) {
                        shelfService.addBook(userId, shelfId, new AddShelfBookRequest(null, bookId, null, null));
                    }
                }
                booksImported++;
            } catch (RuntimeException ex) {
                errors++;
            }
        }

        return new GoodreadsImportResultDto(
                rows.size(),
                booksImported,
                shelvesCreated,
                skipped,
                duplicatesSkipped,
                errors
        );
    }

    private UUID resolveBookId(
            Map<String, String> row,
            String title,
            List<String> authorList,
            String isbn13
    ) {
        Integer pages = parseInt(SimpleCsv.get(row, "Number of Pages"));
        if (StringUtils.hasText(isbn13)) {
            try {
                return importBookResolver.resolveByIsbn(isbn13).id();
            } catch (ResourceNotFoundException ex) {
                return importBookResolver.resolveForImport(title, authorList, pages, isbn13).id();
            }
        }
        return importBookResolver.resolveForImport(title, authorList, pages, null).id();
    }

    private void applyRating(UUID userId, UserBookDto userBook, String ratingRaw) {
        if (!StringUtils.hasText(ratingRaw) || "0".equals(ratingRaw.trim())) {
            return;
        }
        try {
            BigDecimal rating = new BigDecimal(ratingRaw.trim());
            libraryService.update(
                    userId,
                    userBook.id(),
                    new UpdateUserBookRequest(null, rating, null, null, null, null, null)
            );
        } catch (NumberFormatException ignored) {
            // skip invalid rating
        }
    }

    private void applyFinishedDate(UUID userId, UserBookDto userBook, String dateRaw) {
        LocalDate finished = parseDate(dateRaw);
        if (finished != null) {
            libraryService.updateFinishedDateIfRead(userId, userBook.id(), finished);
        }
    }

    private ShelfRef ensureShelf(UUID userId, String shelfName) {
        return shelfService.findShelfIdByOwnerAndName(userId, shelfName.trim())
                .map(id -> new ShelfRef(id, false))
                .orElseGet(() -> {
                    ShelfDto created = shelfService.create(
                            userId, new CreateShelfRequest(
                                    shelfName.trim(),
                                    "Imported from Goodreads",
                                    "📚",
                                    ShelfVisibility.PRIVATE
                            ));
                    return new ShelfRef(created.id(), true);
                });
    }

    private record ShelfRef(UUID id, boolean created) {
    }
}
