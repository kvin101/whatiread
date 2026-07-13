package com.whatiread.recommendation.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RecommendationApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String STATUS = "$.status";
    private static final String ID = "$.id";
    private AuthSession recommender;
    private AuthSession recipient;
    private UUID bookId;

    @BeforeEach
    void setUp() throws Exception {
        recommender = registerUser();
        recipient = registerUser();
        makeFriends(recommender, recipient);
        bookId = createBook(recommender, "Recommend Me " + UUID.randomUUID(), "Author", 300);
    }

    @Test
    void recommendAcceptAndDismissFlow() throws Exception {
        String created = mockMvc.perform(post(ApiPaths.RECOMMENDATIONS)
                        .with(bearer(recommender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toUserId": "%s",
                                  "bookId": "%s",
                                  "message": "You will love this"
                                }
                                """.formatted(recipient.userId(), bookId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(STATUS).value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID recommendationId = UUID.fromString(JsonPath.read(created, ID));

        mockMvc.perform(get(ApiPaths.RECOMMENDATIONS + "/inbox").with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(recommendationId.toString()));

        mockMvc.perform(get(ApiPaths.RECOMMENDATIONS + "/sent").with(bearer(recommender.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(recommendationId.toString()));

        mockMvc.perform(post(ApiPaths.RECOMMENDATIONS + "/{recommendationId}/accept", recommendationId)
                        .with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("ACCEPTED"));

        UUID secondBook = createBook(recommender, "Second Pick " + UUID.randomUUID(), "Author", 150);

        String second = mockMvc.perform(post(ApiPaths.RECOMMENDATIONS)
                        .with(bearer(recommender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toUserId": "%s",
                                  "bookId": "%s"
                                }
                                """.formatted(recipient.userId(), secondBook)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID secondId = UUID.fromString(JsonPath.read(second, ID));

        mockMvc.perform(post(ApiPaths.RECOMMENDATIONS + "/{recommendationId}/dismiss", secondId)
                        .with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("DISMISSED"));

        mockMvc.perform(get(ApiPaths.RECOMMENDATIONS + "/suggestions").with(bearer(recipient.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void batchRecommendCreatesMultiplePendingRecs() throws Exception {
        UUID secondBook = createBook(recommender, "Batch Pick " + UUID.randomUUID(), "Author", 200);

        mockMvc.perform(post(ApiPaths.RECOMMENDATIONS + "/batch")
                        .with(bearer(recommender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toUserId": "%s",
                                  "bookIds": ["%s", "%s"],
                                  "message": "Read these"
                                }
                                """.formatted(recipient.userId(), bookId, secondBook)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        mockMvc.perform(get(ApiPaths.RECOMMENDATIONS + "/inbox").with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void deleteWithdrawsSentRecommendation() throws Exception {
        String created = mockMvc.perform(post(ApiPaths.RECOMMENDATIONS)
                        .with(bearer(recommender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toUserId": "%s",
                                  "bookId": "%s"
                                }
                                """.formatted(recipient.userId(), bookId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID recommendationId = UUID.fromString(JsonPath.read(created, ID));

        mockMvc.perform(delete(ApiPaths.RECOMMENDATIONS + "/{recommendationId}", recommendationId)
                        .with(bearer(recommender.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.RECOMMENDATIONS + "/sent").with(bearer(recommender.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(ApiPaths.RECOMMENDATIONS + "/inbox").with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void recommendRejectedWhenRecipientOptedOut() throws Exception {
        mockMvc.perform(patch(ApiPaths.ME)
                        .with(bearer(recipient.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "acceptRecommendations": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptRecommendations").value(false));

        mockMvc.perform(post(ApiPaths.RECOMMENDATIONS)
                        .with(bearer(recommender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toUserId": "%s",
                                  "bookId": "%s"
                                }
                                """.formatted(recipient.userId(), bookId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shelfRecommendationFlow() throws Exception {
        UUID shelfId = createShelf(recommender, "Share Shelf " + UUID.randomUUID(), "FRIENDS");

        String created = mockMvc.perform(post(ApiPaths.RECOMMENDATIONS)
                        .with(bearer(recommender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toUserId": "%s",
                                  "targetType": "SHELF",
                                  "shelfId": "%s",
                                  "message": "Check this shelf"
                                }
                                """.formatted(recipient.userId(), shelfId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("SHELF"))
                .andExpect(jsonPath("$.shelf.id").value(shelfId.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID recommendationId = UUID.fromString(JsonPath.read(created, ID));

        mockMvc.perform(post(ApiPaths.RECOMMENDATIONS + "/{recommendationId}/accept", recommendationId)
                        .with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("ACCEPTED"));
    }
}
