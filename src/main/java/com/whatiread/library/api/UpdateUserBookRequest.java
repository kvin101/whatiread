package com.whatiread.library.api;

import com.whatiread.library.domain.ReadingStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateUserBookRequest(
        ReadingStatus status,
        @DecimalMin("0.5") @DecimalMax("5.0") BigDecimal rating,
        Boolean clearRating,
        @Min(0) Integer progressPages,
        @Min(0) @Max(100) Short progressPercent,
        LocalDate startedAt,
        LocalDate finishedAt
) {
}
