package com.whatiread.shared.event;

import java.util.UUID;

public record RecommendationAcceptedEvent(UUID userId, UUID bookId) {
}
