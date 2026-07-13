package com.whatiread.goal.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpsertReadingGoalRequest(
        @Min(1) @Max(1000) int targetBooks
) {
}
