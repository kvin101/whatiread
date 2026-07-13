package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfVisibility;

public record UpdateShelfBookRequest(
        Integer position,
        ShelfVisibility visibility
) {
}
