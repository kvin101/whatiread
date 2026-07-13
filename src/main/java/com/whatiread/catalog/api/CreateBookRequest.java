package com.whatiread.catalog.api;

import com.whatiread.catalog.domain.BookSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateBookRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 500) String subtitle,
        @NotEmpty List<@NotBlank @Size(max = 200) String> authors,
        @Size(min = 10, max = 20) String isbn,
        Integer pageCount,
        @Size(max = 2048) String coverUrl,
        String description,
        @Size(max = 200) String externalId,
        BookSource source
) {

    public CreateBookRequest {
        authors = authors != null ? List.copyOf(authors) : List.of();
        if (source == null) {
            source = BookSource.MANUAL;
        }
    }
}
