package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfVisibility;
import jakarta.validation.constraints.Size;

public record UpdateShelfRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 32) String icon,
        ShelfVisibility visibility,
        Integer sortOrder
) {
}
