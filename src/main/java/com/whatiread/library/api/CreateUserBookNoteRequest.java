package com.whatiread.library.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserBookNoteRequest(
        @NotBlank @Size(max = 10000) String body
) {
}
