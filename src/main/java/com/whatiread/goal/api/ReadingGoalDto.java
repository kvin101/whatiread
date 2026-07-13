package com.whatiread.goal.api;

public record ReadingGoalDto(
        short year,
        int targetBooks,
        int booksRead
) {
}
