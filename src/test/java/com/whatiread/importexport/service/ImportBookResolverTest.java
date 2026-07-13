package com.whatiread.importexport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.catalog.service.BookService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportBookResolverTest {


    private static final String GEORGE_ORWELL = "George Orwell";
    @Mock
    private BookService bookService;

    @InjectMocks
    private ImportBookResolver importBookResolver;

    private static BookDto bookDto() {
        return new BookDto(
                UUID.randomUUID(),
                "1984",
                null,
                List.of(GEORGE_ORWELL),
                "9780141036144",
                328,
                null,
                null,
                BookSource.OPEN_LIBRARY,
                "ol-1",
                BigDecimal.valueOf(4.5),
                100,
                null,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z")
        );
    }

    @Test
    void resolveByIsbnDelegatesToBookService() {
        BookDto book = bookDto();
        when(bookService.findOrCreateByIsbn("9780141036144")).thenReturn(book);

        assertThat(importBookResolver.resolveByIsbn("9780141036144")).isSameAs(book);
    }

    @Test
    void resolveForImportDelegatesToBookService() {
        BookDto book = bookDto();
        List<String> authors = List.of(GEORGE_ORWELL);
        when(bookService.findOrCreateForImport("1984", authors, 328, "9780141036144")).thenReturn(book);

        assertThat(importBookResolver.resolveForImport("1984", authors, 328, "9780141036144"))
                .isSameAs(book);
    }
}
