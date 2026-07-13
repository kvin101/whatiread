package com.whatiread.recommendation.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.recommendation.api.CreateBatchRecommendationRequest;
import com.whatiread.recommendation.api.CreateRecommendationRequest;
import com.whatiread.recommendation.api.RecommendationDto;
import com.whatiread.recommendation.api.RecommendationSuggestionDto;
import com.whatiread.recommendation.service.RecommendationService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.RECOMMENDATIONS)
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RecommendationDto create(@CurrentUserId UUID userId, @Valid @RequestBody CreateRecommendationRequest request) {
        return recommendationService.createFriendRecommendation(userId, request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    List<RecommendationDto> createBatch(
            @CurrentUserId UUID userId,
            @Valid @RequestBody CreateBatchRecommendationRequest request
    ) {
        return recommendationService.createBatchFriendRecommendations(userId, request);
    }

    @GetMapping("/inbox")
    List<RecommendationDto> listInbox(@CurrentUserId UUID userId) {
        return recommendationService.listInbox(userId);
    }

    @GetMapping("/sent")
    List<RecommendationDto> listSent(@CurrentUserId UUID userId) {
        return recommendationService.listSent(userId);
    }

    @PostMapping("/{recommendationId}/accept")
    RecommendationDto accept(@CurrentUserId UUID userId, @PathVariable UUID recommendationId) {
        return recommendationService.accept(userId, recommendationId);
    }

    @PostMapping("/{recommendationId}/dismiss")
    RecommendationDto dismiss(@CurrentUserId UUID userId, @PathVariable UUID recommendationId) {
        return recommendationService.dismiss(userId, recommendationId);
    }

    @DeleteMapping("/{recommendationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId UUID userId, @PathVariable UUID recommendationId) {
        recommendationService.delete(userId, recommendationId);
    }

    @GetMapping("/suggestions")
    List<RecommendationSuggestionDto> listSuggestions(@CurrentUserId UUID userId) {
        return recommendationService.listSuggestions(userId);
    }
}
