package com.whatiread.library.api;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.library.domain.ReadingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserBookDto(
        UUID id,
        BookDto book,
        ReadingStatus status,
        BigDecimal rating,
        Integer progressPages,
        Integer pageCount,
        Short progressPercent,
        String progressDisplay,
        LocalDate startedAt,
        LocalDate finishedAt,
        List<UserBookNoteDto> notes,
        Instant createdAt,
        Instant updatedAt
) {
}
