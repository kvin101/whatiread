package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfMemberRole;
import com.whatiread.shelf.domain.ShelfVisibility;
import java.time.Instant;
import java.util.UUID;

public record ShelfDto(
        UUID id,
        String name,
        String slug,
        String description,
        String icon,
        ShelfVisibility visibility,
        int sortOrder,
        UUID ownerId,
        ShelfMemberRole currentUserRole,
        long bookCount,
        Instant createdAt,
        Instant updatedAt,
        String ownerDisplayName
) {
}
