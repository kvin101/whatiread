package com.whatiread.messaging.api;

import com.whatiread.messaging.domain.MentionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MessageMentionDto(
        @NotNull MentionType type,
        @NotNull UUID targetId,
        @NotBlank @Size(max = 200) String label
) {
}
