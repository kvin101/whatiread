package com.whatiread.notification.api;

import com.whatiread.notification.domain.NotificationType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        NotificationType type,
        Map<String, String> payload,
        Instant readAt,
        Instant createdAt
) {
}
