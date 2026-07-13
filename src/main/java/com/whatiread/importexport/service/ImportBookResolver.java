package com.whatiread.importexport.service;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.service.BookService;
import com.whatiread.shared.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImportBookResolver {

    private final BookService bookService;

    public ImportBookResolver(BookService bookService) {
        this.bookService = bookService;
    }

    @CircuitBreaker(name = "catalogImport", fallbackMethod = "resolveFallback")
    public BookDto resolveByIsbn(String isbn) {
        return bookService.findOrCreateByIsbn(isbn);
    }

    @CircuitBreaker(name = "catalogImport", fallbackMethod = "resolveForImportFallback")
    public BookDto resolveForImport(String title, List<String> authors, Integer pageCount, String isbn) {
        return bookService.findOrCreateForImport(title, authors, pageCount, isbn);
    }

    private BookDto resolveFallback(String isbn, Throwable cause) {
        throw new ResourceNotFoundException("Catalog import unavailable");
    }

    private BookDto resolveForImportFallback(
            String title,
            List<String> authors,
            Integer pageCount,
            String isbn,
            Throwable cause
    ) {
        throw new ResourceNotFoundException("Catalog import unavailable");
    }
}
