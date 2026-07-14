package com.whatiread.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.messaging.api.ChatTypingEventDto;
import com.whatiread.messaging.api.MessageMentionDto;
import com.whatiread.messaging.domain.Conversation;
import com.whatiread.messaging.domain.ConversationParticipant;
import com.whatiread.messaging.domain.ConversationParticipantRole;
import com.whatiread.messaging.domain.ConversationType;
import com.whatiread.messaging.domain.MentionType;
import com.whatiread.messaging.domain.Message;
import com.whatiread.messaging.domain.MessageMention;
import com.whatiread.messaging.repository.ConversationParticipantRepository;
import com.whatiread.messaging.repository.ConversationRepository;
import com.whatiread.messaging.repository.ConversationUnreadView;
import com.whatiread.messaging.repository.MessageMentionRepository;
import com.whatiread.messaging.repository.MessageRepository;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shelf.service.ShelfService;
import com.whatiread.social.service.BlockService;
import com.whatiread.social.service.FriendshipService;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class MessagingServiceImplTest {


    private static final String CHECK_THIS = "check this";
    private static final String FAVORITES = "Favorites";
    private static final String HELLO = "hello";
    private static final String HASH = "hash";
    private static final String LOOK = "look";
    private static final String BOOK = "book";
    private static final String HEY = "hey";
    private static final String BOOK_CLUB = "Book Club";
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageMentionRepository messageMentionRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private BlockService blockService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ShelfService shelfService;
    @Mock
    private BookPersistencePort bookPersistencePort;
    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private MessagingServiceImpl messagingService;

    private UUID userId;
    private UUID friendId;
    private UUID thirdId;
    private UUID conversationId;
    private User userA;
    private User userB;
    private User userC;
    private Conversation conversation;
    private Conversation groupConversation;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id on " + entity.getClass());
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        friendId = UUID.randomUUID();
        thirdId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        userA = new User("a@example.com", "alice", HASH, "Alice", "A");
        userB = new User("b@example.com", "bob", HASH, "Bob", "B");
        userC = new User("c@example.com", "carol", HASH, "Carol", "C");
        setId(userA, userId);
        setId(userB, friendId);
        setId(userC, thirdId);
        conversation = new Conversation(userA, userB);
        setId(conversation, conversationId);
        groupConversation = Conversation.createGroup(userA, BOOK_CLUB);
        setId(groupConversation, UUID.randomUUID());
    }

    @Test
    void getOrCreateConversationRequiresFriendship() {
        when(friendshipService.areFriends(userId, friendId)).thenReturn(false);

        assertThatThrownBy(() -> messagingService.getOrCreateConversation(userId, friendId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You can only message friends");
    }

    @Test
    void getOrCreateConversationReturnsExistingConversation() {
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(conversationRepository.findByUserA_IdAndUserB_Id(any(), any())).thenReturn(Optional.of(conversation));
        when(userLookupService.getPersistenceReference(any())).thenReturn(userB);
        when(messageRepository.findTopByConversation_IdOrderBySentAtDesc(conversationId)).thenReturn(Optional.empty());
        when(messageRepository.countByConversation_IdAndSender_IdNotAndReadAtIsNull(conversationId, userId))
                .thenReturn(0L);

        var dto = messagingService.getOrCreateConversation(userId, friendId);

        assertThat(dto.id()).isEqualTo(conversationId);
        assertThat(dto.type()).isEqualTo(ConversationType.DIRECT);
    }

    @Test
    void sendMessageNotifiesAllParticipantsIncludingSender() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        Message message = new Message(conversation, userA, HELLO);
        setId(message, UUID.randomUUID());
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        var dto = messagingService.sendMessage(userId, conversationId, " hello ");

        assertThat(dto.body()).isEqualTo(HELLO);
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/messages"), any());
        verify(messagingTemplate).convertAndSendToUser(eq(friendId.toString()), eq("/queue/messages"), any());
        verify(businessMetrics).recordMessageSent();
    }

    @Test
    void sendGroupMessageNotifiesAllParticipantsIncludingSender() {
        UUID groupId = groupConversation.getId();
        when(conversationRepository.findById(groupId)).thenReturn(Optional.of(groupConversation));
        when(participantRepository.existsByConversation_IdAndUser_Id(groupId, userId)).thenReturn(true);
        when(participantRepository.findUserIdsByConversationId(groupId)).thenReturn(List.of(userId, friendId, thirdId));
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        Message message = new Message(groupConversation, userA, HELLO);
        setId(message, UUID.randomUUID());
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        messagingService.sendMessage(userId, groupId, HELLO);

        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/messages"), any());
        verify(messagingTemplate).convertAndSendToUser(eq(friendId.toString()), eq("/queue/messages"), any());
        verify(messagingTemplate).convertAndSendToUser(eq(thirdId.toString()), eq("/queue/messages"), any());
    }

    @Test
    void createGroupConversationAddsCreatorAndMembers() {
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        when(userLookupService.getPersistenceReference(friendId)).thenReturn(userB);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation saved = invocation.getArgument(0);
            setId(saved, groupConversation.getId());
            return saved;
        });
        when(participantRepository.findByConversation_IdOrderByJoinedAtAsc(groupConversation.getId()))
                .thenReturn(List.of(
                        new ConversationParticipant(groupConversation, userA, ConversationParticipantRole.ADMIN),
                        new ConversationParticipant(groupConversation, userB, ConversationParticipantRole.MEMBER)
                ));
        when(participantRepository.existsByConversationIdAndUserIdAndRole(
                groupConversation.getId(), userId, ConversationParticipantRole.ADMIN)).thenReturn(true);
        when(messageRepository.findTopByConversation_IdOrderBySentAtDesc(groupConversation.getId()))
                .thenReturn(Optional.empty());
        when(messageRepository.countByConversation_IdAndSender_IdNotAndReadAtIsNull(groupConversation.getId(), userId))
                .thenReturn(0L);

        var dto = messagingService.createGroupConversation(userId, BOOK_CLUB, List.of(friendId));

        assertThat(dto.type()).isEqualTo(ConversationType.GROUP);
        assertThat(dto.name()).isEqualTo(BOOK_CLUB);
        assertThat(dto.memberCount()).isEqualTo(2);
        verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
    }

    @Test
    void renameGroupConversationRequiresCreator() {
        UUID groupId = groupConversation.getId();
        when(conversationRepository.findById(groupId)).thenReturn(Optional.of(groupConversation));
        when(participantRepository.existsByConversation_IdAndUser_Id(groupId, friendId)).thenReturn(true);

        assertThatThrownBy(() -> messagingService.renameGroupConversation(friendId, groupId, "New Name"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void addGroupMemberRequiresAdmin() {
        UUID groupId = groupConversation.getId();
        when(conversationRepository.findById(groupId)).thenReturn(Optional.of(groupConversation));
        when(participantRepository.existsByConversation_IdAndUser_Id(groupId, friendId)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserIdAndRole(
                groupId, friendId, ConversationParticipantRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> messagingService.addGroupMember(friendId, groupId, thirdId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void countUnreadMessagesDelegatesToRepository() {
        when(messageRepository.countTotalUnread(userId)).thenReturn(5L);

        assertThat(messagingService.countUnreadMessages(userId)).isEqualTo(5L);
    }

    @Test
    void sendMessageRejectsShelfMentionWithoutAccess() {
        UUID shelfId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        Message message = new Message(conversation, userA, LOOK);
        setId(message, UUID.randomUUID());
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(shelfService.canViewShelf(shelfId, userId)).thenReturn(false);

        assertThatThrownBy(() -> messagingService.sendMessage(
                userId,
                conversationId,
                LOOK,
                List.of(new MessageMentionDto(MentionType.SHELF, shelfId, FAVORITES))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listMessagesDoesNotMarkMessagesRead() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        Message message = new Message(conversation, userB, "hi");
        setId(message, UUID.randomUUID());
        when(messageRepository.findLatestByConversation(eq(conversationId), any(Pageable.class)))
                .thenReturn(List.of(message));
        when(messageMentionRepository.findByMessage_IdIn(any())).thenReturn(List.of());

        var page = messagingService.listMessages(userId, conversationId, null, 20);

        assertThat(page.items()).hasSize(1);
        verify(messageRepository, never()).markAsReadForRecipient(any(), any(), any());
    }

    @Test
    void markConversationReadMarksMessagesRead() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        messagingService.markConversationRead(userId, conversationId);

        verify(messageRepository).markAsReadForRecipient(eq(conversationId), eq(userId), any(Instant.class));
    }

    @Test
    void listConversationsMapsParticipantSummaries() {
        when(conversationRepository.findByParticipant(userId)).thenReturn(List.of(conversation));
        when(userLookupService.getPersistenceReference(friendId)).thenReturn(userB);
        when(messageRepository.findLatestForConversations(List.of(conversationId))).thenReturn(List.of());
        when(messageRepository.countUnreadByConversations(List.of(conversationId), userId))
                .thenReturn(List.of(unreadView(conversationId, 2L)));

        assertThat(messagingService.listConversations(userId)).hasSize(1);
    }

    private static ConversationUnreadView unreadView(UUID conversationId, long count) {
        return new ConversationUnreadView() {
            @Override
            public UUID getConversationId() {
                return conversationId;
            }

            @Override
            public long getUnreadCount() {
                return count;
            }
        };
    }

    @Test
    void publishTypingNotifiesRecipient() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);

        messagingService.publishTyping(userId, conversationId, true);

        verify(messagingTemplate).convertAndSendToUser(
                eq(friendId.toString()), eq("/queue/typing"), any(ChatTypingEventDto.class));
    }

    @Test
    void publishTypingNotifiesAllGroupParticipants() {
        UUID groupId = groupConversation.getId();
        when(conversationRepository.findById(groupId)).thenReturn(Optional.of(groupConversation));
        when(participantRepository.existsByConversation_IdAndUser_Id(groupId, userId)).thenReturn(true);
        when(participantRepository.findUserIdsByConversationId(groupId)).thenReturn(List.of(userId, friendId, thirdId));

        messagingService.publishTyping(userId, groupId, true);

        verify(messagingTemplate).convertAndSendToUser(
                eq(friendId.toString()), eq("/queue/typing"), any(ChatTypingEventDto.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq(thirdId.toString()), eq("/queue/typing"), any(ChatTypingEventDto.class));
    }

    @Test
    void sendMessagePersistsShelfAndBookMentions() {
        UUID shelfId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        Message message = new Message(conversation, userA, CHECK_THIS);
        setId(message, UUID.randomUUID());
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(shelfService.canViewShelf(shelfId, userId)).thenReturn(true);
        when(bookPersistencePort.existsById(bookId)).thenReturn(true);

        var dto = messagingService.sendMessage(
                userId,
                conversationId,
                CHECK_THIS,
                List.of(
                        new MessageMentionDto(MentionType.SHELF, shelfId, FAVORITES),
                        new MessageMentionDto(MentionType.BOOK, bookId, "Dune")));

        assertThat(dto.mentions()).hasSize(2);
        verify(messageMentionRepository, times(2)).save(any(MessageMention.class));
    }

    @Test
    void sendMessageRejectsInvalidUserMention() {
        UUID strangerId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        Message message = new Message(conversation, userA, HEY);
        setId(message, UUID.randomUUID());
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        assertThatThrownBy(() -> messagingService.sendMessage(
                userId,
                conversationId,
                HEY,
                List.of(new MessageMentionDto(MentionType.USER, strangerId, "Stranger"))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sendMessageRejectsMissingBookMention() {
        UUID bookId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        Message message = new Message(conversation, userA, BOOK);
        setId(message, UUID.randomUUID());
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(bookPersistencePort.existsById(bookId)).thenReturn(false);

        assertThatThrownBy(() -> messagingService.sendMessage(
                userId,
                conversationId,
                BOOK,
                List.of(new MessageMentionDto(MentionType.BOOK, bookId, "Missing"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listMessagesUsesHistoryCursorWhenCursorProvided() {
        UUID messageId = UUID.randomUUID();
        Instant sentAt = Instant.parse("2024-06-01T00:00:00Z");
        String cursor = com.whatiread.messaging.util.MessageCursor.encode(sentAt, messageId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        Message message = new Message(conversation, userB, "older");
        setId(message, UUID.randomUUID());
        when(messageRepository.findHistoryBeforeCursor(
                eq(conversationId), eq(sentAt), eq(messageId), any(Pageable.class)))
                .thenReturn(List.of(message));
        when(messageMentionRepository.findByMessage_IdIn(any())).thenReturn(List.of());

        assertThat(messagingService.listMessages(userId, conversationId, cursor, 10).items()).hasSize(1);
    }

    @Test
    void getOrCreateConversationCreatesNewWhenMissing() {
        when(friendshipService.areFriends(userId, friendId)).thenReturn(true);
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(false);
        when(conversationRepository.findByUserA_IdAndUserB_Id(any(), any())).thenReturn(Optional.empty());
        when(userLookupService.getPersistenceReference(userId)).thenReturn(userA);
        when(userLookupService.getPersistenceReference(friendId)).thenReturn(userB);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.findTopByConversation_IdOrderBySentAtDesc(conversationId)).thenReturn(Optional.empty());
        when(messageRepository.countByConversation_IdAndSender_IdNotAndReadAtIsNull(conversationId, userId))
                .thenReturn(0L);

        assertThat(messagingService.getOrCreateConversation(userId, friendId).id()).isEqualTo(conversationId);
    }

    @Test
    void getOrCreateConversationRejectsSelf() {
        assertThatThrownBy(() -> messagingService.getOrCreateConversation(userId, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getOrCreateConversationRejectsBlockedUsers() {
        when(blockService.isBlockedEitherWay(userId, friendId)).thenReturn(true);

        assertThatThrownBy(() -> messagingService.getOrCreateConversation(userId, friendId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void publishTypingRejectsWhenNotFriends() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(friendshipService.areFriends(userId, friendId)).thenReturn(false);

        assertThatThrownBy(() -> messagingService.publishTyping(userId, conversationId, true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void leaveGroupDeletesConversationWhenLastMember() {
        UUID groupId = groupConversation.getId();
        when(conversationRepository.findById(groupId)).thenReturn(Optional.of(groupConversation));
        when(participantRepository.existsByConversation_IdAndUser_Id(groupId, userId)).thenReturn(true);
        when(participantRepository.countByConversation_Id(groupId)).thenReturn(0L);

        messagingService.leaveGroupConversation(userId, groupId);

        verify(participantRepository).deleteByConversation_IdAndUser_Id(groupId, userId);
        verify(conversationRepository).deleteById(groupId);
    }
}
