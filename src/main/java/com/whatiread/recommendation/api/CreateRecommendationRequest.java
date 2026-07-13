package com.whatiread.recommendation.api;

import com.whatiread.recommendation.domain.RecommendationTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateRecommendationRequest(
        @NotNull UUID toUserId,
        RecommendationTargetType targetType,
        UUID bookId,
        UUID shelfId,
        @Size(max = 500) String message
) {
}
