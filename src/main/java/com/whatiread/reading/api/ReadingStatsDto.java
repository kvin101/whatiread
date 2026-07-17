package com.whatiread.reading.api;

public record ReadingStatsDto(
        short year,
        int booksRead,
        int pagesRead,
        Integer targetBooks,
        Integer targetPages,
        Double booksProgressPercent,
        Double pagesProgressPercent
) {
}
