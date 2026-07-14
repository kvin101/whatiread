package com.whatiread.messaging.repository;

import java.util.UUID;

public interface ConversationUnreadView {

    UUID getConversationId();

    long getUnreadCount();
}
