package com.whatiread.messaging.api;

import java.util.UUID;

public record ChatTypingEventDto(
        UUID conversationId,
        UUID userId,
        boolean typing
) {
}
