package com.whatiread.shelf.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ShelfReadingOverlapIntegrationTest extends AbstractApiIntegrationTest {

    private AuthSession owner;
    private AuthSession member;
    private UUID shelfId;
    private UUID bookId;
    private UUID ownerUserBookId;
    private UUID memberUserBookId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerUser();
        member = registerUser();
        makeFriends(owner, member);

        bookId = createBook(owner, "Overlap Book " + UUID.randomUUID(), "Author", 220);
        ownerUserBookId = addToLibrary(owner, bookId);
        memberUserBookId = addToLibrary(member, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + "/{userBookId}", ownerUserBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READING",
                                  "progressPages": 30
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch(ApiPaths.LIBRARY + "/{userBookId}", memberUserBookId)
                        .with(bearer(member.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READING",
                                  "progressPages": 45
                                }
                                """))
                .andExpect(status().isOk());

        shelfId = createShelf(owner, "Overlap Shelf " + UUID.randomUUID(), "PRIVATE");

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/members", shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "role": "VIEWER"
                                }
                                """.formatted(member.userId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/books", shelfId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userBookId": "%s"
                                }
                                """.formatted(ownerUserBookId)))
                .andExpect(status().isCreated());
    }

    @Test
    void listsMembersReadingSameShelfBook() throws Exception {
        mockMvc.perform(get(ApiPaths.SHELVES + "/{shelfId}/reading-overlap", shelfId)
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookId").value(bookId.toString()))
                .andExpect(jsonPath("$[0].readers", hasSize(2)));
    }
}
