package com.whatiread.shared.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.shared.event.RecommendationAcceptedEvent;
import com.whatiread.shared.event.UserRegisteredEvent;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxEventProcessor(
            OutboxEventRepository outboxEventRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${whatiread.outbox.poll-interval-ms:1000}")
    @SchedulerLock(name = "outboxEventProcessor", lockAtMostFor = "PT30S", lockAtLeastFor = "PT1S")
    @Transactional
    public void processPendingEvents() {
        for (OutboxEvent event : outboxEventRepository.findTop50ByProcessedAtIsNullOrderByCreatedAtAsc()) {
            try {
                dispatch(event);
                event.setProcessedAt(java.time.Instant.now());
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                log.warn(
                        "Failed to process outbox event {} (attempt {}): {}",
                        event.getId(), event.getAttempts(), ex.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }

    private void dispatch(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case "UserRegisteredEvent" -> eventPublisher.publishEvent(
                    objectMapper.readValue(event.getPayload(), UserRegisteredEvent.class));
            case "RecommendationAcceptedEvent" -> eventPublisher.publishEvent(
                    objectMapper.readValue(event.getPayload(), RecommendationAcceptedEvent.class));
            default -> throw new IllegalStateException("Unknown outbox event type: " + event.getEventType());
        }
    }
}
