package com.whatiread.importexport.api;

import com.whatiread.importexport.domain.ImportJobStatus;
import java.util.UUID;

public record ImportJobDto(
        UUID jobId,
        ImportJobStatus status,
        GoodreadsImportResultDto result,
        String errorMessage
) {
}
