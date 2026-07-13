package com.whatiread.importexport.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportExportApiIntegrationTest extends AbstractApiIntegrationTest {

    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String TEXT_CSV = "text/csv";
    private static final String APPLICATION_JSON = "application/json";
    private static final String EXPORT_BOOK = "Export Book";
    private AuthSession user;
    private UUID bookId;

    @BeforeEach
    void setUp() throws Exception {
        user = registerUser();
        bookId = createBook(user, "Export Book " + UUID.randomUUID(), "Export Author", 180);
        addToLibrary(user, bookId);
    }

    @Test
    void exportLibraryAsCsvAndJson() throws Exception {
        mockMvc.perform(get(ApiPaths.EXPORT + "/library.csv").with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_DISPOSITION, containsString("library.csv")))
                .andExpect(content().contentTypeCompatibleWith(TEXT_CSV))
                .andExpect(content().string(containsString(EXPORT_BOOK)));

        mockMvc.perform(get(ApiPaths.EXPORT + "/library.json").with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(header().string(CONTENT_DISPOSITION, containsString("library.json")))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().string(containsString(EXPORT_BOOK)));
    }
}
