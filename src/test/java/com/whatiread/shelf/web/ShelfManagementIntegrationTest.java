package com.whatiread.shelf.web;

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

class ShelfManagementIntegrationTest extends AbstractApiIntegrationTest {


    private static final String SHELF_ID_MEMBERS_MEMBER_USER_ID_PATH = "/{shelfId}/members/{memberUserId}";
    private static final String SHELF_ID_MEMBERS_PATH = "/{shelfId}/members";
    private static final String SHELF_ID_BOOKS_PATH = "/{shelfId}/books";
    private static final String SHELF_ID_PATH = "/{shelfId}";
    private AuthSession owner;
    private UUID bookId;
    private UUID userBookId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerUser();
        bookId = createBook(owner, "Shelf Book " + UUID.randomUUID(), "Author", 250);
        userBookId = addToLibrary(owner, bookId);
    }

    @Test
    void shelfLifecycleWithBooksMembersAndClone() throws Exception {
        UUID shelfId = createShelf(owner, "Favorites " + UUID.randomUUID(), "PUBLIC");

        mockMvc.perform(get(ApiPaths.SHELVES).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch(ApiPaths.SHELVES + SHELF_ID_PATH, shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Renamed shelf",
                                  "description": "Updated description"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed shelf"));

        String updatedShelf = mockMvc.perform(get(ApiPaths.SHELVES + SHELF_ID_PATH, shelfId)
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String slug = JsonPath.read(updatedShelf, "$.slug");

        mockMvc.perform(post(ApiPaths.SHELVES + SHELF_ID_BOOKS_PATH, shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userBookId": "%s"
                                }
                                """.formatted(userBookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ApiPaths.SHELVES + SHELF_ID_BOOKS_PATH, shelfId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        AuthSession collaborator = registerUser();
        makeFriends(owner, collaborator);

        mockMvc.perform(post(ApiPaths.SHELVES + SHELF_ID_MEMBERS_PATH, shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "role": "EDITOR"
                                }
                                """.formatted(collaborator.userId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ApiPaths.SHELVES + SHELF_ID_MEMBERS_PATH, shelfId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(patch(ApiPaths.SHELVES + SHELF_ID_MEMBERS_MEMBER_USER_ID_PATH, shelfId, collaborator.userId())
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "VIEWER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        String cloneResponse = mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/clone", shelfId)
                        .with(bearer(collaborator.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cloned shelf"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID cloneId = UUID.fromString(JsonPath.read(cloneResponse, "$.id"));

        mockMvc.perform(get(ApiPaths.SHELVES + "/{shelfId}", cloneId).with(bearer(collaborator.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clonedFromShelfId").value(shelfId.toString()))
                .andExpect(jsonPath("$.clonedFromShelfName").value("Renamed shelf"));

        mockMvc.perform(get(ApiPaths.SHELVES + "/{shelfId}/events", cloneId).with(bearer(collaborator.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.SHELVES + "/system").with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get(ApiPaths.SHELVES + "/system/TO_READ/books").with(bearer(owner.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.PUBLIC_USER_SHELVES, owner.userId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(shelfId.toString())));

        mockMvc.perform(get(ApiPaths.PUBLIC + "/users/{ownerId}/shelves/{slug}", owner.userId(), slug))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.PUBLIC + "/users/{ownerId}/shelves/{slug}/books", owner.userId(), slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete(ApiPaths.SHELVES + "/{shelfId}/books/{userBookId}", shelfId, userBookId)
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(ApiPaths.SHELVES + SHELF_ID_MEMBERS_MEMBER_USER_ID_PATH, shelfId, collaborator.userId())
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isNoContent());
    }
}
