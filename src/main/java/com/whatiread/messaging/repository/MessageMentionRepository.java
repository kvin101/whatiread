package com.whatiread.messaging.repository;

import com.whatiread.messaging.domain.MessageMention;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageMentionRepository extends JpaRepository<MessageMention, UUID> {

    List<MessageMention> findByMessage_Id(UUID messageId);

    List<MessageMention> findByMessage_IdIn(Collection<UUID> messageIds);
}
