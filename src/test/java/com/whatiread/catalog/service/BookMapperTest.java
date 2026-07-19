package com.whatiread.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.whatiread.catalog.domain.Book;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookMapperTest {

    private final BookMapper bookMapper = new BookMapper();

    @Test
    void mapsBookFields() {
        Book book = new Book();
        book.setTitle("Dune");
        book.setAuthors(List.of("Frank Herbert"));
        book.setPageCount(412);
        book.setPublishYear(1965);

        var dto = bookMapper.toDto(book);

        assertThat(dto.title()).isEqualTo("Dune");
        assertThat(dto.authors()).containsExactly("Frank Herbert");
        assertThat(dto.pageCount()).isEqualTo(412);
        assertThat(dto.publishYear()).isEqualTo(1965);
    }

    @Test
    void setAuthorsNullUsesEmptyList() {
        Book book = new Book();
        book.setAuthors(null);

        assertThat(book.getAuthors()).isEmpty();
    }
}
