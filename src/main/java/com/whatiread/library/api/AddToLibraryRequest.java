package com.whatiread.library.api;

import com.whatiread.library.domain.ReadingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddToLibraryRequest(
        @NotNull UUID bookId,
        ReadingStatus status,
        @Min(0) Integer progressPages
) {

    public ReadingStatus statusOrDefault() {
        return status != null ? status : ReadingStatus.TO_READ;
    }
}
