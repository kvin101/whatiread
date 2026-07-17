package com.whatiread.shelf.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.shared.security.SecurityConstants;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ShelfShareIntegrationTest extends AbstractApiIntegrationTest {

    private AuthSession owner;
    private UUID shelfId;
    private UUID bookId;
    private UUID userBookId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerUser();
        bookId = createBook(owner, "Shared Shelf Book " + UUID.randomUUID(), "Author", 200);
        userBookId = addToLibrary(owner, bookId);
        shelfId = createShelf(owner, "Private Share Shelf " + UUID.randomUUID(), "PRIVATE");
        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/books", shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userBookId": "%s"
                                }
                                """.formatted(userBookId)))
                .andExpect(status().isCreated());
    }

    @Test
    void shareLinkViewCloneAndRevoke() throws Exception {
        String linkResponse = mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/share-links", shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(linkResponse, "$.token");
        UUID linkId = UUID.fromString(JsonPath.read(linkResponse, "$.id"));

        mockMvc.perform(get(ApiPaths.PUBLIC_SHELF_SHARE + "/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelf.name").exists())
                .andExpect(jsonPath("$.shelf.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.books", hasSize(1)));

        AuthSession visitor = registerUser();

        String cloneResponse = mockMvc.perform(post(ApiPaths.SHELVES + "/share/{token}/clone", token)
                        .with(bearer(visitor.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cloned from share link"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cloned from share link"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID clonedShelfId = UUID.fromString(JsonPath.read(cloneResponse, "$.id"));

        mockMvc.perform(get(ApiPaths.SHELVES + "/{shelfId}/books", clonedShelfId)
                        .with(bearer(visitor.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete(ApiPaths.SHELVES + "/{shelfId}/share-links/{linkId}", shelfId, linkId)
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.PUBLIC_SHELF_SHARE + "/{token}", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonManagerCannotCreateShareLink() throws Exception {
        AuthSession stranger = registerUser();

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/share-links", shelfId)
                        .with(bearer(stranger.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void secretShelfCannotBeShared() throws Exception {
        UUID secretShelfId = createSecretShelf(owner, "Secret Shelf " + UUID.randomUUID(), "4321");
        String unlockToken = unlockSecretShelf(owner, secretShelfId, "4321");

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/books", secretShelfId)
                        .with(bearer(owner.accessToken()))
                        .header(SecurityConstants.SHELF_UNLOCK_HEADER, unlockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userBookId": "%s"
                                }
                                """.formatted(userBookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/share-links", secretShelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
