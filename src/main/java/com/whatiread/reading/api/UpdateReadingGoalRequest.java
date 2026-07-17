package com.whatiread.reading.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateReadingGoalRequest(
        @NotNull @Min(1900) @Max(2100) Short year,
        @NotNull @Min(1) @Max(10000) Integer targetBooks,
        @Min(1) @Max(1000000) Integer targetPages
) {
}
