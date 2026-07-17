package com.whatiread.reading.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ReadingApiIntegrationTest extends AbstractApiIntegrationTest {

    private AuthSession user;
    private UUID userBookId;

    @BeforeEach
    void setUp() throws Exception {
        user = registerUser();
        UUID bookId = createBook(user, "Stats Book " + UUID.randomUUID(), "Author", 300);
        userBookId = addToLibrary(user, bookId);
    }

    @Test
    void readingGoalStatsAndStreakFlow() throws Exception {
        mockMvc.perform(put(ApiPaths.ME_READING_GOAL)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "year": 2026,
                                  "targetBooks": 24,
                                  "targetPages": 8000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.targetBooks").value(24))
                .andExpect(jsonPath("$.targetPages").value(8000));

        mockMvc.perform(get(ApiPaths.ME_READING_GOAL).param("year", "2026").with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetBooks").value(24));

        mockMvc.perform(patch(ApiPaths.LIBRARY + "/{userBookId}", userBookId)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READING",
                                  "progressPages": 40
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.ME_STREAK).with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get(ApiPaths.ME_STATS).param("year", "2026").with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetBooks").value(24))
                .andExpect(jsonPath("$.pagesRead").exists());
    }

    @Test
    void streakUpdatesWhenAddingBookAsReading() throws Exception {
        UUID freshBookId = createBook(user, "Streak Add Book " + UUID.randomUUID(), "Author", 200);

        mockMvc.perform(post(ApiPaths.LIBRARY)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": "%s",
                                  "status": "READING",
                                  "progressPages": 12
                                }
                                """.formatted(freshBookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ApiPaths.ME_STREAK).with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(greaterThanOrEqualTo(1)));
    }
}
