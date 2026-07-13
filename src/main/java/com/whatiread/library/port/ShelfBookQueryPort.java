package com.whatiread.library.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only shelf membership queries for the library module.
 */
public interface ShelfBookQueryPort {

    List<UUID> findUserBookIdsOnShelf(UUID userId, UUID shelfId);

    Map<UUID, List<String>> getShelfNamesByUserBookIds(UUID userId, List<UUID> userBookIds);

    boolean existsOnShelf(UUID shelfId, UUID userBookId);
}
