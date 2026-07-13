package com.whatiread.messaging.api;

import com.whatiread.messaging.domain.ConversationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        ConversationType type,
        String name,
        ConversationParticipantDto otherParticipant,
        List<ConversationParticipantDto> participants,
        int memberCount,
        UUID createdById,
        boolean viewerIsAdmin,
        boolean viewerIsCreator,
        MessageDto lastMessage,
        Instant createdAt,
        long unreadCount
) {
}
