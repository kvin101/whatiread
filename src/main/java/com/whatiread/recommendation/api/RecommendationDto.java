package com.whatiread.recommendation.api;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.recommendation.domain.RecommendationSource;
import com.whatiread.recommendation.domain.RecommendationStatus;
import com.whatiread.recommendation.domain.RecommendationTargetType;
import com.whatiread.shelf.api.ShelfDto;
import java.time.Instant;
import java.util.UUID;

public record RecommendationDto(
        UUID id,
        RecommendationUserDto fromUser,
        RecommendationUserDto toUser,
        RecommendationTargetType targetType,
        BookDto book,
        ShelfDto shelf,
        String message,
        RecommendationSource source,
        RecommendationStatus status,
        Instant createdAt
) {
}
