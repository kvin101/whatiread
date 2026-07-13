package com.whatiread.messaging.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String body,
        List<MessageMentionDto> mentions,
        Instant sentAt,
        Instant readAt
) {

    public MessageDto {
        mentions = mentions != null ? List.copyOf(mentions) : List.of();
    }
}
