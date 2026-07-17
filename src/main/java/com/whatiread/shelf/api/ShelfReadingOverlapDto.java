package com.whatiread.shelf.api;

import java.util.List;
import java.util.UUID;

public record ShelfReadingOverlapDto(
        UUID bookId,
        String bookTitle,
        List<ShelfReadingMemberDto> readers
) {
    public record ShelfReadingMemberDto(
            UUID userId,
            String displayName
    ) {
    }
}
