package com.whatiread.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.importexport.domain.ImportJob;
import com.whatiread.importexport.domain.ImportJobStatus;
import com.whatiread.importexport.repository.ImportJobRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportJobStatusService {

    private final ImportJobRepository importJobRepository;
    private final ObjectMapper objectMapper;

    public ImportJobStatusService(ImportJobRepository importJobRepository, ObjectMapper objectMapper) {
        this.importJobRepository = importJobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void markProcessing(UUID jobId) {
        ImportJob job = requireJob(jobId);
        job.setStatus(ImportJobStatus.PROCESSING);
        importJobRepository.save(job);
    }

    @Transactional
    public void markCompleted(UUID jobId, GoodreadsImportResultDto result) {
        ImportJob job = requireJob(jobId);
        job.setStatus(ImportJobStatus.COMPLETED);
        try {
            job.setResultJson(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize import result", ex);
        }
        job.setCompletedAt(Instant.now());
        importJobRepository.save(job);
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        ImportJob job = requireJob(jobId);
        job.setStatus(ImportJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(Instant.now());
        importJobRepository.save(job);
    }

    private ImportJob requireJob(UUID jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
    }
}
