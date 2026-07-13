package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfMemberRole;
import java.time.Instant;
import java.util.UUID;

public record ShelfMemberDto(
        UUID id,
        UUID userId,
        String displayName,
        ShelfMemberRole role,
        UUID invitedBy,
        Instant createdAt
) {
}
