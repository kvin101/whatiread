package com.whatiread.importexport.service;

import com.whatiread.config.BusinessMetrics;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.service.ShelfService;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
public class LibraryExportServiceImpl implements LibraryExportService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LibraryService libraryService;
    private final ShelfService shelfService;
    private final BusinessMetrics businessMetrics;

    public LibraryExportServiceImpl(
            LibraryService libraryService,
            ShelfService shelfService,
            BusinessMetrics businessMetrics
    ) {
        this.libraryService = libraryService;
        this.shelfService = shelfService;
        this.businessMetrics = businessMetrics;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public byte[] exportCsv(UUID userId) {
        List<UserBookDto> entries = libraryService.listAllForUser(userId);
        Map<UUID, List<String>> shelfNames = shelfService.getShelfNamesForUserBooks(
                userId,
                entries.stream().map(UserBookDto::id).toList()
        );
        StringBuilder csv = new StringBuilder();
        csv.append("title,authors,isbn,status,rating,progress_pages,finished_at,shelves\n");
        for (UserBookDto entry : entries) {
            String shelves = String.join("; ", shelfNames.getOrDefault(entry.id(), List.of()));
            csv.append(escape(entry.book().title())).append(',');
            csv.append(escape(String.join("; ", entry.book().authors()))).append(',');
            csv.append(escape(entry.book().isbn())).append(',');
            csv.append(entry.status().name()).append(',');
            csv.append(entry.rating() != null ? entry.rating().toPlainString() : "").append(',');
            csv.append(entry.progressPages() != null ? entry.progressPages() : "").append(',');
            csv.append(entry.finishedAt() != null ? entry.finishedAt() : "").append(',');
            csv.append(escape(shelves)).append('\n');
        }
        businessMetrics.recordLibraryExported("csv");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportJson(UUID userId) {
        List<UserBookDto> entries = libraryService.listAllForUser(userId);
        Map<UUID, List<String>> shelfNames = shelfService.getShelfNamesForUserBooks(
                userId,
                entries.stream().map(UserBookDto::id).toList()
        );
        List<Map<String, Object>> payload = entries.stream().map(entry -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookId", entry.book().id());
            row.put("userBookId", entry.id());
            row.put("title", entry.book().title());
            row.put("authors", entry.book().authors());
            row.put("isbn", entry.book().isbn());
            row.put("status", entry.status().name());
            row.put("rating", entry.rating());
            row.put("progressPages", entry.progressPages());
            row.put("finishedAt", entry.finishedAt());
            row.put("shelves", shelfNames.getOrDefault(entry.id(), List.of()));
            return row;
        }).toList();
        try {
            businessMetrics.recordLibraryExported("json");
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Could not export library as JSON", e);
        }
    }
}
