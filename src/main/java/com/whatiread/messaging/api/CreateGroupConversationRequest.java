package com.whatiread.messaging.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateGroupConversationRequest(
        @NotBlank @Size(max = 255) String name,
        @NotEmpty @Size(min = 1, max = 50) List<UUID> memberUserIds
) {
}
