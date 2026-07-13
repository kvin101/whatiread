package com.whatiread.catalog.port;

import com.whatiread.catalog.domain.Book;
import java.util.UUID;

/**
 * Persistence association helper for modules that link to catalog books.
 */
public interface BookPersistencePort {

    boolean existsById(UUID bookId);

    Book getReference(UUID bookId);
}
