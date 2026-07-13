package com.whatiread.importexport.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.importexport.api.ImportJobDto;
import com.whatiread.importexport.service.GoodreadsImportService;
import com.whatiread.importexport.service.LibraryExportService;
import com.whatiread.shared.web.ApiPaths;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.V1)
public class ImportExportController {

    private final GoodreadsImportService goodreadsImportService;
    private final LibraryExportService libraryExportService;

    public ImportExportController(
            GoodreadsImportService goodreadsImportService,
            LibraryExportService libraryExportService
    ) {
        this.goodreadsImportService = goodreadsImportService;
        this.libraryExportService = libraryExportService;
    }

    @PostMapping(value = "/import/goodreads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    ImportJobDto importGoodreads(
            @CurrentUserId UUID userId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are accepted");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("CSV file must be 10 MB or smaller");
        }
        UUID jobId = goodreadsImportService.enqueueImport(userId, file.getBytes());
        return goodreadsImportService.getJob(userId, jobId);
    }

    @GetMapping("/import/goodreads/jobs/{jobId}")
    ImportJobDto getImportJob(@CurrentUserId UUID userId, @PathVariable UUID jobId) {
        return goodreadsImportService.getJob(userId, jobId);
    }

    @GetMapping("/export/library.csv")
    ResponseEntity<byte[]> exportCsv(@CurrentUserId UUID userId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"library.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(libraryExportService.exportCsv(userId));
    }

    @GetMapping("/export/library.json")
    ResponseEntity<byte[]> exportJson(@CurrentUserId UUID userId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"library.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(libraryExportService.exportJson(userId));
    }
}
