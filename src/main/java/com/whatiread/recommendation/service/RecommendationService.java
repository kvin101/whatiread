package com.whatiread.recommendation.service;

import com.whatiread.recommendation.api.CreateBatchRecommendationRequest;
import com.whatiread.recommendation.api.CreateRecommendationRequest;
import com.whatiread.recommendation.api.RecommendationDto;
import com.whatiread.recommendation.api.RecommendationSuggestionDto;
import java.util.List;
import java.util.UUID;

public interface RecommendationService {

    RecommendationDto createFriendRecommendation(UUID fromUserId, CreateRecommendationRequest request);

    List<RecommendationDto> createBatchFriendRecommendations(UUID fromUserId, CreateBatchRecommendationRequest request);

    List<RecommendationDto> listInbox(UUID userId);

    List<RecommendationDto> listSent(UUID userId);

    RecommendationDto accept(UUID userId, UUID recommendationId);

    RecommendationDto dismiss(UUID userId, UUID recommendationId);

    void delete(UUID userId, UUID recommendationId);

    List<RecommendationSuggestionDto> listSuggestions(UUID userId);
}
