package com.whatiread.messaging.api;

import java.util.UUID;

public record ConversationParticipantDto(
        UUID id,
        String displayName,
        String avatarUrl
) {
}
