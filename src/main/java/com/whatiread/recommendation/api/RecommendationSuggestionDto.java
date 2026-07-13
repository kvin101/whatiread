package com.whatiread.recommendation.api;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.recommendation.domain.RecommendationSource;

public record RecommendationSuggestionDto(
        BookDto book,
        RecommendationSource source,
        String reason
) {
}
