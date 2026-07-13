package com.whatiread.library.port;

import com.whatiread.library.domain.UserBook;
import java.util.UUID;

/**
 * Persistence association helper for modules that link to library user books.
 */
public interface UserBookPersistencePort {

    UserBook getOwnedReference(UUID userId, UUID userBookId);
}
