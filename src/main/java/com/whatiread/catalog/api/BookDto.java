package com.whatiread.catalog.api;

import com.whatiread.catalog.domain.BookSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookDto(
        UUID id,
        String title,
        String subtitle,
        List<String> authors,
        String isbn,
        Integer pageCount,
        String coverUrl,
        String description,
        BookSource source,
        String externalId,
        BigDecimal averageRating,
        int ratingCount,
        UUID createdBy,
        UUID updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
