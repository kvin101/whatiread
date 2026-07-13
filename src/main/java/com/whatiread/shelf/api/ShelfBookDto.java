package com.whatiread.shelf.api;

import com.whatiread.library.api.UserBookDto;
import com.whatiread.shelf.domain.ShelfVisibility;
import java.time.Instant;
import java.util.UUID;

public record ShelfBookDto(
        UUID userBookId,
        UserBookDto userBook,
        int position,
        ShelfVisibility visibility,
        ShelfVisibility effectiveVisibility,
        UUID addedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
