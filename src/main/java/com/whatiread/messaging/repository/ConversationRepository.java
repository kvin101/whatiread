package com.whatiread.messaging.repository;

import com.whatiread.messaging.domain.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserA_IdAndUserB_Id(UUID userAId, UUID userBId);

    @Query("""
            SELECT DISTINCT c FROM Conversation c
            LEFT JOIN ConversationParticipant p ON p.conversation = c AND p.user.id = :userId
            WHERE (c.type = com.whatiread.messaging.domain.ConversationType.DIRECT
                   AND (c.userA.id = :userId OR c.userB.id = :userId))
               OR (c.type = com.whatiread.messaging.domain.ConversationType.GROUP AND p.id IS NOT NULL)
            ORDER BY c.createdAt DESC
            """)
    List<Conversation> findByParticipant(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT c FROM Conversation c
            JOIN ConversationParticipant p ON p.conversation = c AND p.user.id = :userId
            WHERE c.type = com.whatiread.messaging.domain.ConversationType.GROUP
            ORDER BY c.createdAt DESC
            """)
    List<Conversation> findGroupsByParticipant(@Param("userId") UUID userId);
}
