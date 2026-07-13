package com.whatiread.importexport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.importexport.domain.ImportJobStatus;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class GoodreadsImportIntegrationTest extends AbstractApiIntegrationTest {

    private static final String JOBS_JOB_ID_PATH = "/jobs/{jobId}";
    private static final String TEXT_CSV = "text/csv";
    private static final String JSON_PATH_JOB_ID = "$.jobId";
    private static final String MULTIPART_FILE_PARAM = "file";

    @Test
    void importGoodreadsCsv() throws Exception {
        AuthSession session = registerUser();
        String csv = """
                Title,Author,Exclusive Shelf,Bookshelves
                "The Hobbit","Tolkien, J.R.R.",read,fantasy
                """;

        MockMultipartFile file = new MockMultipartFile(
                MULTIPART_FILE_PARAM,
                "goodreads.csv",
                TEXT_CSV,
                csv.getBytes()
        );

        String response = mockMvc.perform(multipart(ApiPaths.IMPORT_GOODREADS)
                        .file(file)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID jobId = UUID.fromString(JsonPath.read(response, JSON_PATH_JOB_ID));
        waitForImportJob(session, jobId, 1, 0);
    }

    @Test
    void importSkipsDuplicateRowsInSameFile() throws Exception {
        AuthSession session = registerUser();
        String csv = """
                Title,Author,Exclusive Shelf,Bookshelves
                "Same Book","Author A",to-read,fantasy
                "Same Book","Author A",to-read,fantasy
                """;

        String response = mockMvc.perform(multipart(ApiPaths.IMPORT_GOODREADS)
                        .file(new MockMultipartFile(
                                MULTIPART_FILE_PARAM,
                                "dup.csv",
                                TEXT_CSV,
                                csv.getBytes()))
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID jobId = UUID.fromString(JsonPath.read(response, JSON_PATH_JOB_ID));
        waitForImportJob(session, jobId, 1, 1);
    }

    private void waitForImportJob(AuthSession session, UUID jobId, int booksImported, int duplicatesSkipped)
            throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);
        while (System.currentTimeMillis() < deadline) {
            String body = mockMvc.perform(get(ApiPaths.IMPORT_GOODREADS + JOBS_JOB_ID_PATH, jobId)
                            .with(bearer(session.accessToken())))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String status = JsonPath.read(body, "$.status");
            if (ImportJobStatus.COMPLETED.name().equals(status)) {
                mockMvc.perform(get(ApiPaths.IMPORT_GOODREADS + JOBS_JOB_ID_PATH, jobId)
                                .with(bearer(session.accessToken())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result.booksImported").value(booksImported))
                        .andExpect(jsonPath("$.result.duplicatesSkipped").value(duplicatesSkipped));
                return;
            }
            if (ImportJobStatus.FAILED.name().equals(status)) {
                throw new AssertionError("Import job failed: " + body);
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Import job did not complete in time");
    }
}
