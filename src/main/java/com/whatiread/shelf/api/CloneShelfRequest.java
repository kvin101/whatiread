package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloneShelfRequest(
        @NotBlank @Size(max = 100) String name,
        Boolean includeBooks,
        ShelfVisibility visibility
) {

    public boolean includeBooksOrDefault() {
        return includeBooks == null || includeBooks;
    }

    public ShelfVisibility visibilityOrDefault() {
        return visibility != null ? visibility : ShelfVisibility.PRIVATE;
    }
}
