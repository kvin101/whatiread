package com.whatiread.comment.web;

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

class CommentApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String COMMENT_ID_PATH = "/{commentId}";
    private static final String PARAM_TARGET_TYPE = "targetType";
    private static final String JSON_PATH_CONTENT = "$.content";
    private static final String PARAM_TARGET_ID = "targetId";
    private static final String JSON_PATH_BODY = "$.body";
    private static final String TARGET_TYPE_BOOK = "BOOK";
    private AuthSession user;
    private UUID bookId;
    private UUID userBookId;
    private UUID shelfId;

    @BeforeEach
    void setUp() throws Exception {
        user = registerUser();
        bookId = createBook(user, "Comment Test " + UUID.randomUUID(), "Test Author", 200);
        userBookId = addToLibrary(user, bookId);
        shelfId = createShelf(user, "Comment Shelf " + UUID.randomUUID(), "PRIVATE");
    }

    @Test
    void commentCrudOnBookShelfAndLibraryEntry() throws Exception {
        String bookComment = mockMvc.perform(post(ApiPaths.COMMENTS)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "BOOK",
                                  "targetId": "%s",
                                  "body": "Great catalog entry"
                                }
                                """.formatted(bookId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_BODY).value("Great catalog entry"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID bookCommentId = UUID.fromString(JsonPath.read(bookComment, JSON_PATH_ID));

        mockMvc.perform(get(ApiPaths.COMMENTS)
                        .param(PARAM_TARGET_TYPE, TARGET_TYPE_BOOK)
                        .param(PARAM_TARGET_ID, bookId.toString())
                        .with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1)));

        mockMvc.perform(patch(ApiPaths.COMMENTS + COMMENT_ID_PATH, bookCommentId)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Updated book note"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_BODY).value("Updated book note"));

        String shelfComment = mockMvc.perform(post(ApiPaths.COMMENTS)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "SHELF",
                                  "targetId": "%s",
                                  "body": "Shelf discussion"
                                }
                                """.formatted(shelfId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID shelfCommentId = UUID.fromString(JsonPath.read(shelfComment, JSON_PATH_ID));

        mockMvc.perform(post(ApiPaths.COMMENTS + "/{commentId}/report", shelfCommentId)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Spam"
                                }
                                """))
                .andExpect(status().isAccepted());

        mockMvc.perform(post(ApiPaths.COMMENTS)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "USER_BOOK",
                                  "targetId": "%s",
                                  "body": "Reading notes"
                                }
                                """.formatted(userBookId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(ApiPaths.COMMENTS + COMMENT_ID_PATH, bookCommentId)
                        .with(bearer(user.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.COMMENTS)
                        .param(PARAM_TARGET_TYPE, TARGET_TYPE_BOOK)
                        .param(PARAM_TARGET_ID, bookId.toString())
                        .with(bearer(user.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(0)));
    }
}
