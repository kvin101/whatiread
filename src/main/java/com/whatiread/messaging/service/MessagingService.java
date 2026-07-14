package com.whatiread.messaging.service;

import com.whatiread.messaging.api.ConversationDto;
import com.whatiread.messaging.api.MessageDto;
import com.whatiread.messaging.api.MessageMentionDto;
import com.whatiread.shared.api.CursorPage;
import java.util.List;
import java.util.UUID;

public interface MessagingService {

    ConversationDto getOrCreateConversation(UUID userId, UUID friendUserId);

    ConversationDto createGroupConversation(UUID creatorId, String name, List<UUID> memberUserIds);

    ConversationDto renameGroupConversation(UUID userId, UUID conversationId, String name);

    ConversationDto addGroupMember(UUID adminId, UUID conversationId, UUID memberUserId);

    void removeGroupMember(UUID adminId, UUID conversationId, UUID memberUserId);

    void leaveGroupConversation(UUID userId, UUID conversationId);

    List<ConversationDto> listConversations(UUID userId);

    List<ConversationDto> listGroupConversations(UUID userId);

    CursorPage<MessageDto> listMessages(UUID userId, UUID conversationId, String cursor, int limit);

    MessageDto sendMessage(UUID senderId, UUID conversationId, String body);

    MessageDto sendMessage(UUID senderId, UUID conversationId, String body, List<MessageMentionDto> mentions);

    void publishTyping(UUID userId, UUID conversationId, boolean typing);

    long countUnreadMessages(UUID userId);

    void markConversationRead(UUID userId, UUID conversationId);
}
