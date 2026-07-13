package com.whatiread.messaging.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChatTypingPayload(
        @NotNull UUID conversationId,
        boolean typing
) {
}
