package com.whatiread.messaging.service;

import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.messaging.api.ChatTypingEventDto;
import com.whatiread.messaging.api.ConversationDto;
import com.whatiread.messaging.api.ConversationParticipantDto;
import com.whatiread.messaging.api.MessageDto;
import com.whatiread.messaging.api.MessageMentionDto;
import com.whatiread.messaging.domain.Conversation;
import com.whatiread.messaging.domain.ConversationParticipant;
import com.whatiread.messaging.domain.ConversationParticipantRole;
import com.whatiread.messaging.domain.ConversationType;
import com.whatiread.messaging.domain.Message;
import com.whatiread.messaging.domain.MessageMention;
import com.whatiread.messaging.repository.ConversationParticipantRepository;
import com.whatiread.messaging.repository.ConversationRepository;
import com.whatiread.messaging.repository.MessageMentionRepository;
import com.whatiread.messaging.repository.MessageRepository;
import com.whatiread.messaging.util.ConversationParticipants;
import com.whatiread.messaging.util.MessageCursor;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shelf.service.ShelfService;
import com.whatiread.social.service.BlockService;
import com.whatiread.social.service.FriendshipService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MessagingServiceImpl implements MessagingService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MessageMentionRepository messageMentionRepository;
    private final UserLookupService userLookupService;
    private final FriendshipService friendshipService;
    private final BlockService blockService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ShelfService shelfService;
    private final BookPersistencePort bookPersistencePort;
    private final BusinessMetrics businessMetrics;

    public MessagingServiceImpl(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            MessageMentionRepository messageMentionRepository,
            UserLookupService userLookupService,
            FriendshipService friendshipService,
            BlockService blockService,
            SimpMessagingTemplate messagingTemplate,
            ShelfService shelfService,
            BookPersistencePort bookPersistencePort,
            BusinessMetrics businessMetrics
    ) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.messageMentionRepository = messageMentionRepository;
        this.userLookupService = userLookupService;
        this.friendshipService = friendshipService;
        this.blockService = blockService;
        this.messagingTemplate = messagingTemplate;
        this.shelfService = shelfService;
        this.bookPersistencePort = bookPersistencePort;
        this.businessMetrics = businessMetrics;
    }

    @Override
    public ConversationDto getOrCreateConversation(UUID userId, UUID friendUserId) {
        requireFriends(userId, friendUserId);
        Conversation conversation = findOrCreateConversationEntity(userId, friendUserId);
        return toConversationDto(conversation, userId);
    }

    @Override
    public ConversationDto createGroupConversation(UUID creatorId, String name, List<UUID> memberUserIds) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }
        Set<UUID> uniqueMembers = new LinkedHashSet<>(memberUserIds);
        uniqueMembers.remove(creatorId);
        if (uniqueMembers.isEmpty()) {
            throw new IllegalArgumentException("At least one other member is required");
        }
        for (UUID memberId : uniqueMembers) {
            requireFriends(creatorId, memberId);
        }

        User creator = userLookupService.getPersistenceReference(creatorId);
        Conversation conversation = conversationRepository.save(Conversation.createGroup(creator, trimmedName));
        participantRepository.save(new ConversationParticipant(
                conversation,
                creator,
                ConversationParticipantRole.ADMIN
        ));
        for (UUID memberId : uniqueMembers) {
            User member = userLookupService.getPersistenceReference(memberId);
            participantRepository.save(new ConversationParticipant(
                    conversation,
                    member,
                    ConversationParticipantRole.MEMBER
            ));
        }
        return toConversationDto(conversation, creatorId);
    }

    @Override
    public ConversationDto renameGroupConversation(UUID userId, UUID conversationId, String name) {
        Conversation conversation = getGroupConversation(conversationId, userId);
        if (conversation.getCreatedBy() == null || !conversation.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Only the group creator can rename this group");
        }
        conversation.setName(name);
        return toConversationDto(conversation, userId);
    }

    @Override
    public ConversationDto addGroupMember(UUID adminId, UUID conversationId, UUID memberUserId) {
        Conversation conversation = getGroupConversation(conversationId, adminId);
        requireGroupAdmin(conversationId, adminId);
        if (adminId.equals(memberUserId)) {
            throw new IllegalArgumentException("Cannot add yourself to the group");
        }
        if (participantRepository.existsByConversation_IdAndUser_Id(conversationId, memberUserId)) {
            return toConversationDto(conversation, adminId);
        }
        requireFriends(adminId, memberUserId);
        User member = userLookupService.getPersistenceReference(memberUserId);
        participantRepository.save(new ConversationParticipant(
                conversation,
                member,
                ConversationParticipantRole.MEMBER
        ));
        return toConversationDto(conversation, adminId);
    }

    @Override
    public void removeGroupMember(UUID adminId, UUID conversationId, UUID memberUserId) {
        getGroupConversation(conversationId, adminId);
        requireGroupAdmin(conversationId, adminId);
        if (adminId.equals(memberUserId)) {
            throw new IllegalArgumentException("Use leave to remove yourself from the group");
        }
        if (!participantRepository.existsByConversation_IdAndUser_Id(conversationId, memberUserId)) {
            throw new ResourceNotFoundException("Member not in group");
        }
        participantRepository.deleteByConversation_IdAndUser_Id(conversationId, memberUserId);
    }

    @Override
    public void leaveGroupConversation(UUID userId, UUID conversationId) {
        getGroupConversation(conversationId, userId);
        participantRepository.deleteByConversation_IdAndUser_Id(conversationId, userId);
        if (participantRepository.countByConversation_Id(conversationId) == 0) {
            conversationRepository.deleteById(conversationId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findByParticipant(userId);
        if (conversations.isEmpty()) {
            return List.of();
        }
        List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();
        Map<UUID, MessageDto> lastMessages = loadLastMessages(conversationIds);
        Map<UUID, Long> unreadCounts = loadUnreadCounts(conversationIds, userId);
        return conversations.stream()
                .map(conversation -> toConversationDto(
                        conversation,
                        userId,
                        lastMessages.get(conversation.getId()),
                        unreadCounts.getOrDefault(conversation.getId(), 0L)
                ))
                .sorted((left, right) -> {
                    Instant leftAt = left.lastMessage() != null ? left.lastMessage().sentAt() : left.createdAt();
                    Instant rightAt = right.lastMessage() != null ? right.lastMessage().sentAt() : right.createdAt();
                    return rightAt.compareTo(leftAt);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> listGroupConversations(UUID userId) {
        return conversationRepository.findGroupsByParticipant(userId).stream()
                .map(conversation -> toConversationDto(conversation, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MessageDto> listMessages(UUID userId, UUID conversationId, String cursor, int limit) {
        Conversation conversation = getParticipantConversation(conversationId, userId);
        int pageSize = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<Message> messages;
        if (cursor == null || cursor.isBlank()) {
            messages = messageRepository.findLatestByConversation(conversation.getId(), pageable);
        } else {
            MessageCursor.Parts parts = MessageCursor.decode(cursor);
            messages = messageRepository.findHistoryBeforeCursor(
                    conversation.getId(),
                    parts.sentAt(),
                    parts.id(),
                    pageable
            );
        }
        boolean hasMore = messages.size() > pageSize;
        List<Message> page = hasMore ? messages.subList(0, pageSize) : messages;
        Map<UUID, List<MessageMentionDto>> mentionsByMessage = loadMentionsByMessageId(
                page.stream().map(Message::getId).toList()
        );
        List<MessageDto> dtos = new ArrayList<>(page.size());
        for (Message message : page) {
            dtos.add(toMessageDto(message, mentionsByMessage.getOrDefault(message.getId(), List.of())));
        }
        Collections.reverse(dtos);
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            Message oldest = page.getLast();
            nextCursor = MessageCursor.encode(oldest.getSentAt(), oldest.getId());
        }
        return new CursorPage<>(dtos, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(UUID userId) {
        return messageRepository.countTotalUnread(userId);
    }

    @Override
    public void markConversationRead(UUID userId, UUID conversationId) {
        Conversation conversation = getParticipantConversation(conversationId, userId);
        messageRepository.markAsReadForRecipient(conversation.getId(), userId, Instant.now());
    }

    @Override
    public MessageDto sendMessage(UUID senderId, UUID conversationId, String body) {
        return sendMessage(senderId, conversationId, body, List.of());
    }

    @Override
    public MessageDto sendMessage(UUID senderId, UUID conversationId, String body, List<MessageMentionDto> mentions) {
        Conversation conversation = getParticipantConversation(conversationId, senderId);
        requireCanMessage(conversation, senderId);
        User sender = userLookupService.getPersistenceReference(senderId);
        Message message = messageRepository.save(new Message(conversation, sender, body.trim()));
        Set<UUID> participantIds = getParticipantUserIds(conversation);
        List<MessageMentionDto> savedMentions = saveMentions(message, senderId, participantIds, mentions);
        MessageDto dto = toMessageDto(message, savedMentions);
        notifyParticipants(senderId, participantIds, dto);
        businessMetrics.recordMessageSent();
        return dto;
    }

    private List<MessageMentionDto> saveMentions(
            Message message,
            UUID senderId,
            Set<UUID> participantIds,
            List<MessageMentionDto> mentions
    ) {
        if (mentions == null || mentions.isEmpty()) {
            return List.of();
        }
        List<MessageMentionDto> saved = new ArrayList<>();
        for (MessageMentionDto mention : mentions) {
            validateMention(senderId, participantIds, mention);
            messageMentionRepository.save(new MessageMention(
                    message,
                    mention.type(),
                    mention.targetId(),
                    mention.label().trim()
            ));
            saved.add(mention);
        }
        return saved;
    }

    private void validateMention(UUID senderId, Set<UUID> participantIds, MessageMentionDto mention) {
        switch (mention.type()) {
            case USER -> {
                if (!participantIds.contains(mention.targetId()) && !mention.targetId().equals(senderId)) {
                    throw new ForbiddenException("Can only mention chat participants");
                }
            }
            case SHELF -> {
                if (!shelfService.canViewShelf(mention.targetId(), senderId)) {
                    throw new ForbiddenException("Cannot mention shelf you cannot access");
                }
            }
            case BOOK -> {
                if (!bookPersistencePort.existsById(mention.targetId())) {
                    throw new ResourceNotFoundException("Book not found");
                }
            }
        }
    }

    private Conversation findOrCreateConversationEntity(UUID userId, UUID friendUserId) {
        ConversationParticipants.OrderedPair pair = ConversationParticipants.order(userId, friendUserId);
        return conversationRepository.findByUserA_IdAndUserB_Id(pair.userAId(), pair.userBId())
                .orElseGet(() -> {
                    User userA = userLookupService.getPersistenceReference(pair.userAId());
                    User userB = userLookupService.getPersistenceReference(pair.userBId());
                    return conversationRepository.save(new Conversation(userA, userB));
                });
    }

    private Conversation getParticipantConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!isParticipant(conversation, userId)) {
            throw new ForbiddenException("Not a participant in this conversation");
        }
        return conversation;
    }

    private Conversation getGroupConversation(UUID conversationId, UUID userId) {
        Conversation conversation = getParticipantConversation(conversationId, userId);
        if (!conversation.isGroup()) {
            throw new IllegalArgumentException("Not a group conversation");
        }
        return conversation;
    }

    private boolean isParticipant(Conversation conversation, UUID userId) {
        if (conversation.isDirect()) {
            return conversation.involves(userId);
        }
        return participantRepository.existsByConversation_IdAndUser_Id(conversation.getId(), userId);
    }

    private Set<UUID> getParticipantUserIds(Conversation conversation) {
        if (conversation.isDirect()) {
            return Set.of(conversation.getUserA().getId(), conversation.getUserB().getId());
        }
        return new HashSet<>(participantRepository.findUserIdsByConversationId(conversation.getId()));
    }

    private void requireGroupAdmin(UUID conversationId, UUID userId) {
        if (!participantRepository.existsByConversationIdAndUserIdAndRole(
                conversationId,
                userId,
                ConversationParticipantRole.ADMIN
        )) {
            throw new ForbiddenException("Only group admins can manage members");
        }
    }

    private void requireCanMessage(Conversation conversation, UUID userId) {
        if (conversation.isDirect()) {
            requireFriends(userId, conversation.otherParticipantId(userId));
        }
    }

    private void requireFriends(UUID userId, UUID friendUserId) {
        if (userId.equals(friendUserId)) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }
        if (blockService.isBlockedEitherWay(userId, friendUserId)) {
            throw new ForbiddenException("Cannot message this user");
        }
        if (!friendshipService.areFriends(userId, friendUserId)) {
            throw new ForbiddenException("You can only message friends");
        }
    }

    private ConversationDto toConversationDto(Conversation conversation, UUID viewerId) {
        MessageDto lastMessage = messageRepository.findTopByConversation_IdOrderBySentAtDesc(conversation.getId())
                .map(this::toMessageDto)
                .orElse(null);
        long unread = messageRepository.countByConversation_IdAndSender_IdNotAndReadAtIsNull(
                conversation.getId(),
                viewerId
        );
        return toConversationDto(conversation, viewerId, lastMessage, unread);
    }

    private ConversationDto toConversationDto(
            Conversation conversation,
            UUID viewerId,
            MessageDto lastMessage,
            long unread
    ) {
        if (conversation.isDirect()) {
            UUID otherId = conversation.otherParticipantId(viewerId);
            User other = userLookupService.getPersistenceReference(otherId);
            return new ConversationDto(
                    conversation.getId(),
                    ConversationType.DIRECT,
                    null,
                    toParticipantDto(other),
                    List.of(),
                    2,
                    null,
                    false,
                    false,
                    lastMessage,
                    conversation.getCreatedAt(),
                    unread
            );
        }

        List<ConversationParticipantDto> participants = participantRepository
                .findByConversation_IdOrderByJoinedAtAsc(conversation.getId())
                .stream()
                .map(p -> toParticipantDto(p.getUser()))
                .toList();
        boolean viewerIsAdmin = participantRepository.existsByConversationIdAndUserIdAndRole(
                conversation.getId(),
                viewerId,
                ConversationParticipantRole.ADMIN
        );
        boolean viewerIsCreator = conversation.getCreatedBy() != null
                && conversation.getCreatedBy().getId().equals(viewerId);

        return new ConversationDto(
                conversation.getId(),
                ConversationType.GROUP,
                conversation.getName(),
                null,
                participants,
                participants.size(),
                conversation.getCreatedBy() != null ? conversation.getCreatedBy().getId() : null,
                viewerIsAdmin,
                viewerIsCreator,
                lastMessage,
                conversation.getCreatedAt(),
                unread
        );
    }

    private ConversationParticipantDto toParticipantDto(User user) {
        return new ConversationParticipantDto(user.getId(), user.getDisplayName(), user.getAvatarUrl());
    }

    private MessageDto toMessageDto(Message message) {
        return toMessageDto(message, loadMentionsByMessageId(List.of(message.getId()))
                .getOrDefault(message.getId(), List.of()));
    }

    private Map<UUID, MessageDto> loadLastMessages(List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        List<Message> messages = messageRepository.findLatestForConversations(conversationIds);
        Map<UUID, List<MessageMentionDto>> mentionsByMessage = loadMentionsByMessageId(
                messages.stream().map(Message::getId).toList()
        );
        Map<UUID, MessageDto> result = new HashMap<>();
        for (Message message : messages) {
            result.put(
                    message.getConversation().getId(),
                    toMessageDto(message, mentionsByMessage.getOrDefault(message.getId(), List.of()))
            );
        }
        return result;
    }

    private Map<UUID, Long> loadUnreadCounts(List<UUID> conversationIds, UUID userId) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        messageRepository.countUnreadByConversations(conversationIds, userId)
                .forEach(view -> counts.put(view.getConversationId(), view.getUnreadCount()));
        return counts;
    }

    private Map<UUID, List<MessageMentionDto>> loadMentionsByMessageId(List<UUID> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return messageMentionRepository.findByMessage_IdIn(messageIds).stream()
                .collect(Collectors.groupingBy(
                        mention -> mention.getMessage().getId(),
                        Collectors.mapping(
                                mention -> new MessageMentionDto(
                                        mention.getMentionType(),
                                        mention.getTargetId(),
                                        mention.getLabel()
                                ),
                                Collectors.toList()
                        )
                ));
    }

    private MessageDto toMessageDto(Message message, List<MessageMentionDto> mentions) {
        return new MessageDto(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getBody(),
                mentions,
                message.getSentAt(),
                message.getReadAt()
        );
    }

    private void notifyParticipants(UUID senderId, Set<UUID> participantIds, MessageDto dto) {
        for (UUID participantId : participantIds) {
            messagingTemplate.convertAndSendToUser(participantId.toString(), "/queue/messages", dto);
        }
    }

    private void notifyTyping(UUID senderId, Set<UUID> participantIds, ChatTypingEventDto event) {
        for (UUID participantId : participantIds) {
            if (!participantId.equals(senderId)) {
                messagingTemplate.convertAndSendToUser(participantId.toString(), "/queue/typing", event);
            }
        }
    }

    @Override
    public void publishTyping(UUID userId, UUID conversationId, boolean typing) {
        Conversation conversation = getParticipantConversation(conversationId, userId);
        requireCanMessage(conversation, userId);
        Set<UUID> participantIds = getParticipantUserIds(conversation);
        ChatTypingEventDto event = new ChatTypingEventDto(conversationId, userId, typing);
        notifyTyping(userId, participantIds, event);
    }
}
