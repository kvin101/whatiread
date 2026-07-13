package com.whatiread.library.api;

import java.time.Instant;
import java.util.UUID;

public record UserBookNoteDto(
        UUID id,
        String body,
        UUID authorId,
        Instant createdAt,
        Instant updatedAt
) {
}
