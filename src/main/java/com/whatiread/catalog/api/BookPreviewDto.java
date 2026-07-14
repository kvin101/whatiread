package com.whatiread.catalog.api;

import com.whatiread.catalog.domain.BookSource;
import java.math.BigDecimal;
import java.util.List;

public record BookPreviewDto(
        String title,
        String subtitle,
        List<String> authors,
        Integer pageCount,
        String isbn,
        String coverUrl,
        String description,
        Integer publishYear,
        List<String> subjects,
        BigDecimal averageRating,
        Integer ratingCount,
        BookSource source,
        String externalId
) {
}
