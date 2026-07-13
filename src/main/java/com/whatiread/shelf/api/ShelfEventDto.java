package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ShelfEventDto(
        UUID id,
        ShelfEventType eventType,
        UUID actorId,
        String actorDisplayName,
        Map<String, String> payload,
        Instant createdAt
) {
}
