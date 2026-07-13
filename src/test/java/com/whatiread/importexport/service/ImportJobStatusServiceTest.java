package com.whatiread.importexport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.importexport.domain.ImportJob;
import com.whatiread.importexport.domain.ImportJobStatus;
import com.whatiread.importexport.repository.ImportJobRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportJobStatusServiceTest {

    private static final String PARSE_ERROR = "parse error";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private ImportJobRepository importJobRepository;
    @InjectMocks
    private ImportJobStatusService importJobStatusService;
    private UUID jobId;
    private ImportJob job;

    @BeforeEach
    void setUp() {
        importJobStatusService = new ImportJobStatusService(importJobRepository, objectMapper);
        jobId = UUID.randomUUID();
        job = new ImportJob();
        job.setUserId(UUID.randomUUID());
        job.setStatus(ImportJobStatus.PENDING);
    }

    @Test
    void markProcessingUpdatesStatus() {
        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        importJobStatusService.markProcessing(jobId);

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.PROCESSING);
        verify(importJobRepository).save(job);
    }

    @Test
    void markCompletedPersistsSerializedResult() {
        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        GoodreadsImportResultDto result = new GoodreadsImportResultDto(10, 3, 1, 2, 1, 0);

        importJobStatusService.markCompleted(jobId, result);

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.COMPLETED);
        assertThat(job.getResultJson()).contains("\"booksImported\"");
        assertThat(job.getCompletedAt()).isNotNull();
        verify(importJobRepository).save(job);
    }

    @Test
    void markFailedStoresErrorMessage() {
        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        importJobStatusService.markFailed(jobId, PARSE_ERROR);

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo(PARSE_ERROR);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void requiresExistingJob() {
        when(importJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importJobStatusService.markProcessing(jobId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Import job not found");
    }
}
