package com.whatiread.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.importexport.api.ImportJobDto;
import com.whatiread.importexport.domain.ImportJob;
import com.whatiread.importexport.domain.ImportJobStatus;
import com.whatiread.importexport.repository.ImportJobRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GoodreadsImportJobService {

    private final ImportJobRepository importJobRepository;
    private final GoodreadsCsvProcessor csvProcessor;
    private final ObjectMapper objectMapper;

    public GoodreadsImportJobService(
            ImportJobRepository importJobRepository,
            GoodreadsCsvProcessor csvProcessor,
            ObjectMapper objectMapper
    ) {
        this.importJobRepository = importJobRepository;
        this.csvProcessor = csvProcessor;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID enqueue(UUID userId, byte[] csvBytes) {
        ImportJob job = new ImportJob();
        job.setUserId(userId);
        job.setStatus(ImportJobStatus.PENDING);
        job = importJobRepository.save(job);
        UUID jobId = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                csvProcessor.processJob(jobId, userId, csvBytes);
            }
        });
        return jobId;
    }

    @Transactional(readOnly = true)
    public ImportJobDto getJob(UUID userId, UUID jobId) {
        ImportJob job = importJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
        return toDto(job);
    }

    private ImportJobDto toDto(ImportJob job) {
        GoodreadsImportResultDto result = null;
        if (job.getResultJson() != null) {
            try {
                result = objectMapper.readValue(job.getResultJson(), GoodreadsImportResultDto.class);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Invalid import job result payload", ex);
            }
        }
        return new ImportJobDto(job.getId(), job.getStatus(), result, job.getErrorMessage());
    }
}
