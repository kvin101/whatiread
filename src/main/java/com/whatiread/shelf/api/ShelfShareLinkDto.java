package com.whatiread.shelf.api;

import java.time.Instant;
import java.util.UUID;

public record ShelfShareLinkDto(
        UUID id,
        UUID token,
        UUID shelfId,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        boolean active
) {
}
