package com.whatiread.catalog.service;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.api.CreateBookRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

    Page<BookSearchResultDto> search(String query, Pageable pageable);

    BookDto getById(UUID bookId);

    /**
     * Find an existing catalog book by identity, or create one if none exists.
     */
    BookDto createManual(CreateBookRequest request);

    /**
     * Resolve a catalog row id to the canonical book for the same work (title + authors, ISBN, or external id).
     */
    UUID resolveCanonicalBookId(UUID bookId);

    BookDto findOrCreateByIsbn(String isbn);

    /**
     * Match by ISBN, then title+authors, before creating a manual catalog row.
     */
    BookDto findOrCreateForImport(String title, java.util.List<String> authors, Integer pageCount, String isbn);

    BookDto getOrThrow(UUID bookId);

    void refreshAggregatedRating(UUID bookId);
}
