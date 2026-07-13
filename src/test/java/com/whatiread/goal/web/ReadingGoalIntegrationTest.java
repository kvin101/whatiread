package com.whatiread.goal.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ReadingGoalIntegrationTest extends AbstractApiIntegrationTest {


    private static final String JSON_PATH_TARGET_BOOKS = "$.targetBooks";
    private static final String YEAR = "/{year}";

    @Test
    void upsertAndGetReadingGoal() throws Exception {
        AuthSession session = registerUser();
        short year = 2026;

        mockMvc.perform(put(ApiPaths.GOALS + YEAR, year)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetBooks\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_TARGET_BOOKS).value(12))
                .andExpect(jsonPath("$.booksRead").value(0));

        mockMvc.perform(get(ApiPaths.GOALS + YEAR, year).with(bearer(session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath(JSON_PATH_TARGET_BOOKS).value(12));
    }
}
