package com.whatiread.activity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ActivityApiIntegrationTest extends AbstractApiIntegrationTest {

    private AuthSession viewer;
    private AuthSession friend;

    @BeforeEach
    void setUp() throws Exception {
        viewer = registerUser();
        friend = registerUser();
        makeFriends(viewer, friend);
    }

    @Test
    void listsFriendShelfActivity() throws Exception {
        UUID shelfId = createShelf(friend, "Friend Activity " + UUID.randomUUID(), "FRIENDS");
        UUID bookId = createBook(friend, "Activity Book " + UUID.randomUUID(), "Author", 200);
        UUID userBookId = addToLibrary(friend, bookId);

        mockMvc.perform(post(ApiPaths.SHELVES + "/{shelfId}/books", shelfId)
                        .with(bearer(friend.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userBookId": "%s"
                                }
                                """.formatted(userBookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ApiPaths.ACTIVITY).with(bearer(viewer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasMore").exists());
    }
}
