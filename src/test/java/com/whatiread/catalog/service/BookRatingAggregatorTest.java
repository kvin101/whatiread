package com.whatiread.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookRatingAggregatorTest {

    private static final String AVERAGE_RATING_450 = "4.50";

    @Test
    void aggregateEmptyRatings() {
        var result = BookRatingAggregator.aggregate(List.of());
        assertThat(result.averageRating()).isNull();
        assertThat(result.ratingCount()).isZero();
    }

    @Test
    void aggregateSingleRating() {
        var result = BookRatingAggregator.aggregate(List.of(new BigDecimal("4.5")));
        assertThat(result.averageRating()).isEqualByComparingTo(AVERAGE_RATING_450);
        assertThat(result.ratingCount()).isEqualTo(1);
    }

    @Test
    void aggregateMultipleRatingsRoundsHalfUp() {
        var result = BookRatingAggregator.aggregate(List.of(
                new BigDecimal("4.0"),
                new BigDecimal("5.0")
        ));
        assertThat(result.averageRating()).isEqualByComparingTo(AVERAGE_RATING_450);
        assertThat(result.ratingCount()).isEqualTo(2);
    }
}
