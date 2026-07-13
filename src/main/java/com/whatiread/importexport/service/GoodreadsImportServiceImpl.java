package com.whatiread.importexport.service;

import com.whatiread.identity.service.UserLookupService;
import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.importexport.api.ImportJobDto;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GoodreadsImportServiceImpl implements GoodreadsImportService {

    private final UserLookupService userLookupService;
    private final GoodreadsImportJobService importJobService;
    private final GoodreadsCsvProcessor csvProcessor;

    public GoodreadsImportServiceImpl(
            UserLookupService userLookupService,
            GoodreadsImportJobService importJobService,
            GoodreadsCsvProcessor csvProcessor
    ) {
        this.userLookupService = userLookupService;
        this.importJobService = importJobService;
        this.csvProcessor = csvProcessor;
    }

    @Override
    public UUID enqueueImport(UUID userId, byte[] csvBytes) {
        userLookupService.requireExists(userId);
        return importJobService.enqueue(userId, csvBytes);
    }

    @Override
    public ImportJobDto getJob(UUID userId, UUID jobId) {
        return importJobService.getJob(userId, jobId);
    }

    @Override
    public GoodreadsImportResultDto importCsv(UUID userId, InputStream csvStream) {
        return csvProcessor.importCsv(userId, csvStream);
    }
}
