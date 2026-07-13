package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateShelfMemberRequest(
        @NotNull ShelfMemberRole role
) {
}
