package com.whatiread.shelf.api;

import com.whatiread.library.domain.ReadingStatus;

public record SystemShelfDto(
        ReadingStatus status,
        String label
) {
}
