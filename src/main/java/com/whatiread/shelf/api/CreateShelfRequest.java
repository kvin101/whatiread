package com.whatiread.shelf.api;

import com.whatiread.shelf.domain.ShelfVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShelfRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 32) String icon,
        ShelfVisibility visibility,
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits") String pin
) {

    public ShelfVisibility visibilityOrDefault() {
        return visibility != null ? visibility : ShelfVisibility.PRIVATE;
    }
}
