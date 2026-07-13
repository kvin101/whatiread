package com.whatiread.catalog.integration;

import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.catalog.repository.BookRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookPersistencePortImpl implements BookPersistencePort {

    private final BookRepository bookRepository;

    public BookPersistencePortImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public boolean existsById(UUID bookId) {
        return bookRepository.existsById(bookId);
    }

    @Override
    public Book getReference(UUID bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }
}
