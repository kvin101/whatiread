package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ExploreShelfSource;
import com.whatiread.shelf.domain.ShelfVisibility;
import java.time.Instant;
import java.util.UUID;

public record ExploreShelfDto(
        UUID id,
        String name,
        String slug,
        String description,
        String icon,
        ShelfVisibility visibility,
        ExploreShelfSource source,
        long bookCount,
        UUID ownerId,
        String ownerDisplayName,
        Instant updatedAt
) {
}
