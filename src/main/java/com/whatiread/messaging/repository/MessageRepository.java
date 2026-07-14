package com.whatiread.messaging.repository;

import com.whatiread.messaging.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :conversationId
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<Message> findLatestByConversation(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :conversationId
              AND m.sentAt < :before
            ORDER BY m.sentAt DESC
            """)
    List<Message> findHistoryBefore(
            @Param("conversationId") UUID conversationId,
            @Param("before") Instant before,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM Message m
            WHERE m.conversation.id = :conversationId
              AND (
                m.sentAt < :cursorTime
                OR (m.sentAt = :cursorTime AND m.id < :cursorId)
              )
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<Message> findHistoryBeforeCursor(
            @Param("conversationId") UUID conversationId,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    Optional<Message> findTopByConversation_IdOrderBySentAtDesc(UUID conversationId);

    long countByConversation_IdAndSender_IdNotAndReadAtIsNull(UUID conversationId, UUID userId);

    @Modifying
    @Query("""
            UPDATE Message m SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    int markAsReadForRecipient(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("readAt") Instant readAt
    );

    @Query("""
            SELECT COUNT(m) FROM Message m
            JOIN m.conversation c
            LEFT JOIN ConversationParticipant p ON p.conversation = c AND p.user.id = :userId
            WHERE (
                (c.type = com.whatiread.messaging.domain.ConversationType.DIRECT
                 AND (c.userA.id = :userId OR c.userB.id = :userId))
                OR (c.type = com.whatiread.messaging.domain.ConversationType.GROUP AND p.id IS NOT NULL)
            )
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    long countTotalUnread(@Param("userId") UUID userId);
}
