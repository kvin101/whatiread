package com.whatiread.recommendation.api;

import com.whatiread.recommendation.domain.RecommendationTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateBatchRecommendationRequest(
        @NotNull UUID toUserId,
        RecommendationTargetType targetType,
        @Size(max = 20) List<UUID> bookIds,
        @Size(max = 20) List<UUID> shelfIds,
        @Size(max = 500) String message
) {
}
