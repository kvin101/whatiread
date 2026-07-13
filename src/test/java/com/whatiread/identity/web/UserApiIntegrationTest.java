package com.whatiread.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String JSON_PATH_EMAIL = "$.email";

    @Test
    void profileAndMeEndpoints() throws Exception {
        AuthSession user = registerUser();
        UUID shelfId = createShelf(user, "Profile Shelf " + UUID.randomUUID(), "PUBLIC");

        mockMvc.perform(get(ApiPaths.ME).with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_EMAIL).value(user.email()));

        mockMvc.perform(patch(ApiPaths.ME)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "lastName": "Reader",
                                  "writer": true,
                                  "writerBio": "I read a lot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.writer").value(true));

        AuthSession viewer = registerUser();

        mockMvc.perform(get(ApiPaths.USERS + "/{userId}/profile", user.userId())
                        .with(bearer(viewer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.userId().toString()));

        mockMvc.perform(get(ApiPaths.USERS + "/{userId}/shelves", user.userId())
                        .with(bearer(viewer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(shelfId.toString())));
    }

    @Test
    void refreshTokenRotation() throws Exception {
        AuthSession session = registerUser();

        String refreshResponse = mockMvc.perform(post(ApiPaths.AUTH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ACCESS_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_REFRESH_TOKEN).isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newAccessToken = com.jayway.jsonpath.JsonPath.read(refreshResponse, JSON_PATH_ACCESS_TOKEN);

        mockMvc.perform(get(ApiPaths.ME).with(bearer(newAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_EMAIL).value(session.email()));
    }
}
