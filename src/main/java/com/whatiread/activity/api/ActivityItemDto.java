package com.whatiread.activity.api;

import com.whatiread.shelf.domain.ShelfEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ActivityItemDto(
        UUID id,
        ShelfEventType eventType,
        UUID actorId,
        String actorDisplayName,
        UUID shelfId,
        String shelfName,
        UUID shelfOwnerId,
        String shelfOwnerDisplayName,
        Map<String, String> payload,
        Instant createdAt
) {
}
