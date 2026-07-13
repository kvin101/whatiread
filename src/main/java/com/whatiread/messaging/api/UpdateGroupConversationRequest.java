package com.whatiread.messaging.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupConversationRequest(
        @NotBlank @Size(max = 255) String name
) {
}
