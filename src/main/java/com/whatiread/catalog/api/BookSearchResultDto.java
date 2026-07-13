package com.whatiread.catalog.api;

import com.whatiread.catalog.domain.BookSource;
import java.util.List;
import java.util.UUID;

public record BookSearchResultDto(
        UUID id,
        String title,
        List<String> authors,
        String isbn,
        Integer pageCount,
        String coverUrl,
        BookSource source,
        String externalId
) {
}
