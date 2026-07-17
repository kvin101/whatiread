package com.whatiread.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.notification.api.NotificationDto;
import com.whatiread.notification.domain.Notification;
import com.whatiread.notification.domain.NotificationType;
import com.whatiread.notification.repository.NotificationRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    private final NotificationRepository notificationRepository;
    private final UserLookupService userLookupService;
    private final ObjectMapper objectMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserLookupService userLookupService,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.userLookupService = userLookupService;
        this.objectMapper = objectMapper;
    }

    public void notifyFriendRequest(UUID recipientId, UUID requestId, UUID requesterId, String requesterName) {
        create(recipientId, NotificationType.FRIEND_REQUEST, Map.of(
                "requestId", requestId.toString(),
                "requesterId", requesterId.toString(),
                "requesterName", requesterName
        ));
    }

    public void notifyRecommendation(UUID recipientId, UUID recommendationId, UUID fromUserId, String fromUserName) {
        create(recipientId, NotificationType.RECOMMENDATION, Map.of(
                "recommendationId", recommendationId.toString(),
                "fromUserId", fromUserId.toString(),
                "fromUserName", fromUserName
        ));
    }

    public void notifyMention(UUID recipientId, UUID messageId, UUID conversationId, UUID senderId, String senderName) {
        create(recipientId, NotificationType.MENTION, Map.of(
                "messageId", messageId.toString(),
                "conversationId", conversationId.toString(),
                "senderId", senderId.toString(),
                "senderName", senderName
        ));
    }

    public void create(UUID userId, NotificationType type, Map<String, String> payload) {
        User user = userLookupService.getPersistenceReference(userId);
        notificationRepository.save(new Notification(user, type, serializePayload(payload)));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(UUID userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                deserializePayload(notification.getPayload()),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private String serializePayload(Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid notification payload", ex);
        }
    }

    private Map<String, String> deserializePayload(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }
}
