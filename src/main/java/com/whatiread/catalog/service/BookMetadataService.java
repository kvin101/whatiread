package com.whatiread.catalog.service;

import com.whatiread.catalog.api.BookPreviewDto;
import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.catalog.integration.OpenLibraryClient;
import com.whatiread.catalog.integration.OpenLibraryDoc;
import com.whatiread.catalog.integration.OpenLibraryText;
import com.whatiread.config.CacheConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BookMetadataService {

    private static final String COVER_URL_TEMPLATE = "https://covers.openlibrary.org/b/id/%d-M.jpg";

    private final OpenLibraryClient openLibraryClient;

    public BookMetadataService(OpenLibraryClient openLibraryClient) {
        this.openLibraryClient = openLibraryClient;
    }

    static String extractIsbn(List<String> isbns) {
        if (isbns == null) {
            return null;
        }
        return isbns.stream()
                .filter(isbn -> isbn != null && isbn.length() == 13)
                .findFirst()
                .orElseGet(() -> isbns.stream()
                        .filter(isbn -> isbn != null && !isbn.isBlank())
                        .findFirst()
                        .orElse(null));
    }

    static String buildCoverUrl(Long coverId) {
        return coverId != null ? COVER_URL_TEMPLATE.formatted(coverId) : null;
    }

    @Cacheable(cacheNames = CacheConfig.OPEN_LIBRARY_SEARCH, key = "#query + ':' + #page + ':' + #size")
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "searchFallback")
    public List<BookSearchResultDto> searchExternal(String query, int page, int size) {
        var response = openLibraryClient.search(query, size, page + 1);
        if (response.docs() == null) {
            return List.of();
        }
        List<BookSearchResultDto> results = new ArrayList<>();
        for (OpenLibraryDoc doc : response.docs()) {
            results.add(toSearchResult(doc));
        }
        return results;
    }

    @SuppressWarnings("unused")
    private List<BookSearchResultDto> searchFallback(String query, int page, int size, Throwable cause) {
        return List.of();
    }

    @Cacheable(cacheNames = CacheConfig.OPEN_LIBRARY_PREVIEW, key = "#externalId")
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "previewFallback")
    public BookPreviewDto getExternalPreview(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
        String normalized = externalId.startsWith("/") ? externalId.substring(1) : externalId;
        if (normalized.startsWith("works/")) {
            return mapWorkPreview(openLibraryClient.getWork(normalized.substring("works/".length())), externalId);
        }
        if (normalized.startsWith("books/")) {
            return mapEditionPreview(openLibraryClient.getEdition(normalized.substring("books/".length())), externalId);
        }
        throw new IllegalArgumentException("Unsupported Open Library id: " + externalId);
    }

    @SuppressWarnings("unused")
    private BookPreviewDto previewFallback(String externalId, Throwable cause) {
        return null;
    }

    private BookPreviewDto mapWorkPreview(Map<String, Object> payload, String externalId) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        return new BookPreviewDto(
                text(payload.get("title")),
                text(payload.get("subtitle")),
                List.of(),
                intValue(payload.get("number_of_pages")),
                null,
                null,
                OpenLibraryText.extract(payload.get("description")),
                OpenLibraryText.parsePublishYear(payload.get("first_publish_date")),
                stringList(payload.get("subjects")),
                null,
                null,
                BookSource.OPEN_LIBRARY,
                externalId
        );
    }

    private BookPreviewDto mapEditionPreview(Map<String, Object> payload, String externalId) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        List<String> isbns = stringList(payload.get("isbn_13"));
        if (isbns.isEmpty()) {
            isbns = stringList(payload.get("isbn_10"));
        }
        return new BookPreviewDto(
                text(payload.get("title")),
                null,
                List.of(),
                intValue(payload.get("number_of_pages")),
                extractIsbn(isbns),
                BookMetadataService.buildCoverUrl(longValue(payload.get("covers"))),
                OpenLibraryText.extract(payload.get("description")),
                OpenLibraryText.parsePublishYear(payload.get("publish_date")),
                stringList(payload.get("subjects")),
                null,
                null,
                BookSource.OPEN_LIBRARY,
                externalId
        );
    }

    private static String text(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Long longValue(Object value) {
        if (value instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                String text = item.toString().trim();
                if (!text.isBlank()) {
                    results.add(text);
                }
            }
        }
        return results;
    }

    public void applyOpenLibraryDoc(Book book, OpenLibraryDoc doc) {
        book.setTitle(doc.title());
        book.setAuthors(doc.authorName() != null ? doc.authorName() : List.of());
        book.setPageCount(doc.pageCount());
        book.setCoverUrl(buildCoverUrl(doc.coverId()));
        book.setSource(BookSource.OPEN_LIBRARY);
        book.setExternalId(doc.key());
        book.setIsbn(extractIsbn(doc.isbn()));
    }

    private BookSearchResultDto toSearchResult(OpenLibraryDoc doc) {
        return new BookSearchResultDto(
                null,
                doc.title(),
                doc.authorName() != null ? doc.authorName() : List.of(),
                extractIsbn(doc.isbn()),
                doc.pageCount(),
                buildCoverUrl(doc.coverId()),
                BookSource.OPEN_LIBRARY,
                doc.key()
        );
    }
}
