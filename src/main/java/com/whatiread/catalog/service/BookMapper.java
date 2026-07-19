package com.whatiread.catalog.service;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.Book;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getSubtitle(),
                List.copyOf(book.getAuthors()),
                book.getIsbn(),
                book.getPageCount(),
                book.getPublishYear(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getSource(),
                book.getExternalId(),
                book.getAverageRating(),
                book.getRatingCount(),
                book.getCreatedBy(),
                book.getUpdatedBy(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
