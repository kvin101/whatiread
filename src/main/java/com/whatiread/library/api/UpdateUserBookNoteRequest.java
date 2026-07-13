package com.whatiread.library.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserBookNoteRequest(
        @NotBlank @Size(max = 10000) String body
) {
}
