package com.whatiread.recommendation.api;

import java.util.UUID;

public record RecommendationUserDto(
        UUID id,
        String displayName,
        String avatarUrl
) {
}
