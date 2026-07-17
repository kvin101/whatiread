package com.whatiread.reading.api;

public record ReadingGoalDto(
        short year,
        int targetBooks,
        Integer targetPages
) {
}
