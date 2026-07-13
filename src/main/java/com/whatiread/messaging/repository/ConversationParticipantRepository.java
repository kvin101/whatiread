package com.whatiread.messaging.repository;

import com.whatiread.messaging.domain.ConversationParticipant;
import com.whatiread.messaging.domain.ConversationParticipantRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    List<ConversationParticipant> findByConversation_IdOrderByJoinedAtAsc(UUID conversationId);

    boolean existsByConversation_IdAndUser_Id(UUID conversationId, UUID userId);

    Optional<ConversationParticipant> findByConversation_IdAndUser_Id(UUID conversationId, UUID userId);

    long countByConversation_Id(UUID conversationId);

    void deleteByConversation_IdAndUser_Id(UUID conversationId, UUID userId);

    @Query("""
            SELECT p.user.id FROM ConversationParticipant p
            WHERE p.conversation.id = :conversationId
            """)
    List<UUID> findUserIdsByConversationId(@Param("conversationId") UUID conversationId);

    @Query("""
            SELECT COUNT(p) > 0 FROM ConversationParticipant p
            WHERE p.conversation.id = :conversationId
              AND p.user.id = :userId
              AND p.role = :role
            """)
    boolean existsByConversationIdAndUserIdAndRole(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("role") ConversationParticipantRole role
    );
}
