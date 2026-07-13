package com.whatiread.catalog.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class BookRatingAggregator {

    private BookRatingAggregator() {
    }

    public static AggregatedRating aggregate(List<BigDecimal> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return new AggregatedRating(null, 0);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal rating : ratings) {
            sum = sum.add(rating);
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        return new AggregatedRating(average, ratings.size());
    }

    public record AggregatedRating(BigDecimal averageRating, int ratingCount) {
    }
}
