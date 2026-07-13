package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfVisibility;
import java.util.UUID;

public record AddShelfBookRequest(
        UUID userBookId,
        UUID bookId,
        Integer position,
        ShelfVisibility visibility
) {
}
