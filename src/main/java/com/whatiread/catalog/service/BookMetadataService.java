package com.whatiread.catalog.service;

import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.catalog.integration.OpenLibraryClient;
import com.whatiread.catalog.integration.OpenLibraryDoc;
import com.whatiread.config.CacheConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.ArrayList;
import java.util.List;
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
