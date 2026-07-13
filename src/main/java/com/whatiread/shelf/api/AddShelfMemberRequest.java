package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfMemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddShelfMemberRequest(
        @NotNull UUID userId,
        @NotNull ShelfMemberRole role
) {
}
