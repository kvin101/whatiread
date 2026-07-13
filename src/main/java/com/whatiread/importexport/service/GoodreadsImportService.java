package com.whatiread.importexport.service;

import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.importexport.api.ImportJobDto;
import java.util.UUID;

public interface GoodreadsImportService {

    UUID enqueueImport(UUID userId, byte[] csvBytes);

    ImportJobDto getJob(UUID userId, UUID jobId);

    GoodreadsImportResultDto importCsv(UUID userId, java.io.InputStream csvStream);
}
