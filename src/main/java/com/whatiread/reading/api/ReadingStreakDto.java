package com.whatiread.reading.api;

import java.time.LocalDate;

public record ReadingStreakDto(
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate
) {
}
