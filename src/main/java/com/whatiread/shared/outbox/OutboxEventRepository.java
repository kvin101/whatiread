package com.whatiread.shared.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByProcessedAtIsNullOrderByCreatedAtAsc();

    long countByProcessedAtIsNull();

    List<OutboxEvent> findByProcessedAtIsNullAndCreatedAtBefore(Instant cutoff);
}
