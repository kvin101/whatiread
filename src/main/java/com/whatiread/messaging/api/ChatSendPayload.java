package com.whatiread.messaging.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ChatSendPayload(
        @NotNull UUID conversationId,
        @NotBlank @Size(max = 4000) String body,
        List<@Valid MessageMentionDto> mentions
) {

    public ChatSendPayload {
        mentions = mentions != null ? List.copyOf(mentions) : List.of();
    }
}
