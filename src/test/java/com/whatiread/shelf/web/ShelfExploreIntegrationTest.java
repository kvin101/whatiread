package com.whatiread.shelf.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ShelfExploreIntegrationTest extends AbstractApiIntegrationTest {


    private static final String CONTENT_ID = "$.content[*].id";
    private static final String EXPLORE = "/explore";
    private static final String ID = "$.id";
    private static final String CONTENT_ID_S_SOURCE = "$.content[?(@.id=='%s')].source";

    @Test
    void exploreIncludesOtherUsersPublicShelves() throws Exception {
        AuthSession owner = registerUser();
        String createResponse = mockMvc.perform(post(ApiPaths.SHELVES)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Public picks",
                                  "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shelfId = com.jayway.jsonpath.JsonPath.read(createResponse, ID);

        AuthSession viewer = registerUser();

        mockMvc.perform(get(ApiPaths.SHELVES + EXPLORE).with(bearer(viewer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONTENT_ID, hasItem(shelfId)))
                .andExpect(jsonPath(CONTENT_ID_S_SOURCE.formatted(shelfId), hasItem("PUBLIC")));
    }

    @Test
    void exploreExcludesOwnShelves() throws Exception {
        AuthSession owner = registerUser();
        String createResponse = mockMvc.perform(post(ApiPaths.SHELVES)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "My public list",
                                  "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shelfId = com.jayway.jsonpath.JsonPath.read(createResponse, ID);

        mockMvc.perform(get(ApiPaths.SHELVES + EXPLORE).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONTENT_ID, not(hasItem(shelfId))));
    }

    @Test
    void exploreIncludesShelvesSharedViaMembership() throws Exception {
        AuthSession owner = registerUser();
        AuthSession collaborator = registerUser();

        String createResponse = mockMvc.perform(post(ApiPaths.SHELVES)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Team shelf",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shelfId = com.jayway.jsonpath.JsonPath.read(createResponse, ID);

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/members", shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "role": "VIEWER"
                                }
                                """.formatted(collaborator.userId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ApiPaths.SHELVES + EXPLORE).with(bearer(collaborator.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONTENT_ID, hasItem(shelfId)))
                .andExpect(jsonPath(CONTENT_ID_S_SOURCE.formatted(shelfId), hasItem("SHARED")));
    }
}
