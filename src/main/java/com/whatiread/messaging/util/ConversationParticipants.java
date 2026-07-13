package com.whatiread.messaging.util;

import java.util.UUID;

public final class ConversationParticipants {

    private ConversationParticipants() {
    }

    public static OrderedPair order(UUID first, UUID second) {
        if (first.compareTo(second) <= 0) {
            return new OrderedPair(first, second);
        }
        return new OrderedPair(second, first);
    }

    public record OrderedPair(UUID userAId, UUID userBId) {
    }
}
