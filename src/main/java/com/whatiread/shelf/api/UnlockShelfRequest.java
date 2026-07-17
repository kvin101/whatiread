package com.whatiread.shelf.api;

import jakarta.validation.constraints.Pattern;

public record UnlockShelfRequest(
        @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
        String pin
) {
}
