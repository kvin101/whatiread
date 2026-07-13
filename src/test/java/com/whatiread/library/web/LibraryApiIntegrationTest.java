package com.whatiread.library.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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

class LibraryApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String USER_BOOK_ID_PATH = "/{userBookId}";
    private static final String JSON_PATH_DETAIL = "$.detail";
    private static final String USER_BOOK_ID_NOTES_PATH = "/{userBookId}/notes";
    private static final String JSON_PATH_AVERAGE_RATING = "$.averageRating";
    private static final String JSON_PATH_RATING_COUNT = "$.ratingCount";
    private static final String JSON_PATH_CONTENT = "$.content";
    private static final String BOOK_ID_PATH = "/{bookId}";
    private static final String USER_BOOK_ID_NOTES_NOTE_ID_PATH = "/{userBookId}/notes/{noteId}";
    private static final String BOOK_ALREADY_IN_LIBRARY = "Book already in library";
    private static final String HARRY_PORTER_TRIVIA = "Harry Porter Trivia";
    private static final String JSON_PATH_PROGRESS_DISPLAY = "$.progressDisplay";
    private static final String WINSTON_STARR = "Winston Starr";
    private AuthSession owner;
    private UUID bookId;

    @BeforeEach
    void setUpLibraryFixtures() throws Exception {
        owner = registerUser();
        bookId = createBook(owner, "Neuromancer " + UUID.randomUUID(), "William Gibson", 271);
    }

    @Test
    void addBookToLibrary() throws Exception {
        mockMvc.perform(post(ApiPaths.LIBRARY)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": "%s",
                                  "status": "TO_READ"
                                }
                                """.formatted(bookId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.book.id").value(bookId.toString()))
                .andExpect(jsonPath("$.status").value("TO_READ"))
                .andExpect(jsonPath(JSON_PATH_PROGRESS_DISPLAY).doesNotExist());
    }

    @Test
    void addDuplicateBookReturnsConflict() throws Exception {
        addToLibrary(owner, bookId);

        mockMvc.perform(post(ApiPaths.LIBRARY)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": "%s"
                                }
                                """.formatted(bookId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value(BOOK_ALREADY_IN_LIBRARY));
    }

    @Test
    void addSameWorkReturnsConflictWhenCatalogEntryIsReused() throws Exception {
        UUID catalogId = createBook(owner, HARRY_PORTER_TRIVIA, WINSTON_STARR, 100);
        UUID reusedCatalogId = createBook(owner, HARRY_PORTER_TRIVIA, WINSTON_STARR, 100);
        addToLibrary(owner, catalogId);

        mockMvc.perform(post(ApiPaths.LIBRARY)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": "%s"
                                }
                                """.formatted(reusedCatalogId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value(BOOK_ALREADY_IN_LIBRARY));
    }

    @Test
    void listLibraryEntriesAndFilterByStatus() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(get(ApiPaths.LIBRARY).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(userBookId.toString()));

        mockMvc.perform(get(ApiPaths.LIBRARY).param("status", "READING").with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(0)));

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READING",
                                  "progressPages": 50
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.LIBRARY).param("status", "READING").with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1)));
    }

    @Test
    void updateProgressComputesPercentFromPageCount() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READING",
                                  "progressPages": 68
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPages").value(68))
                .andExpect(jsonPath("$.pageCount").value(271))
                .andExpect(jsonPath("$.progressPercent").value(25))
                .andExpect(jsonPath(JSON_PATH_PROGRESS_DISPLAY).value("68 / 271 pages (25%)"))
                .andExpect(jsonPath("$.startedAt", notNullValue()));
    }

    @Test
    void updateRatingAcceptsHalfStarIncrements() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4.5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    void updateRatingRefreshesBookAggregate() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);
        AuthSession friend = registerUser();
        UUID friendUserBookId = addToLibrary(friend, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4.0
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, friendUserBookId)
                        .with(bearer(friend.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5.0
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.BOOKS + BOOK_ID_PATH, bookId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_AVERAGE_RATING).value(4.5))
                .andExpect(jsonPath(JSON_PATH_RATING_COUNT).value(2));
    }

    @Test
    void clearRatingRefreshesBookAggregate() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4.5
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clearRating": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").doesNotExist());

        mockMvc.perform(get(ApiPaths.BOOKS + BOOK_ID_PATH, bookId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_AVERAGE_RATING).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_RATING_COUNT).value(0));
    }

    @Test
    void deleteLibraryEntryRefreshesBookAggregate() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);
        AuthSession friend = registerUser();
        addToLibrary(friend, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4.0
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId).with(bearer(owner.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.BOOKS + BOOK_ID_PATH, bookId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_AVERAGE_RATING).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_RATING_COUNT).value(0));
    }

    @Test
    void updateRatingRejectsInvalidIncrement() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4.3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value("Rating must use 0.5 increments"));
    }

    @Test
    void notesCrud() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        String noteResponse = mockMvc.perform(post(ApiPaths.LIBRARY + USER_BOOK_ID_NOTES_PATH, userBookId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Cyberpunk before cyberpunk had a name."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Cyberpunk before cyberpunk had a name."))
                .andExpect(jsonPath("$.authorId").value(owner.userId().toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID noteId = UUID.fromString(JsonPath.read(noteResponse, "$.id"));

        mockMvc.perform(get(ApiPaths.LIBRARY + USER_BOOK_ID_NOTES_PATH, userBookId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(noteId.toString()));

        mockMvc.perform(patch(ApiPaths.LIBRARY + USER_BOOK_ID_NOTES_NOTE_ID_PATH, userBookId, noteId)
                        .with(bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Still holds up."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Still holds up."));

        mockMvc.perform(delete(ApiPaths.LIBRARY + USER_BOOK_ID_NOTES_NOTE_ID_PATH, userBookId, noteId)
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.LIBRARY + USER_BOOK_ID_NOTES_PATH, userBookId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listWithCursorPaginatesLibrary() throws Exception {
        UUID book2 = createBook(owner, "Cursor B " + UUID.randomUUID(), "Author B", 120);
        UUID book3 = createBook(owner, "Cursor C " + UUID.randomUUID(), "Author C", 130);
        addToLibrary(owner, bookId);
        addToLibrary(owner, book2);
        addToLibrary(owner, book3);

        String firstPage = mockMvc.perform(get(ApiPaths.LIBRARY)
                        .param("cursor", " ")
                        .param("limit", "2")
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String nextCursor = JsonPath.read(firstPage, "$.nextCursor");

        mockMvc.perform(get(ApiPaths.LIBRARY)
                        .param("cursor", nextCursor)
                        .param("limit", "2")
                        .with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void getLibraryEntryByBookId() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(get(ApiPaths.LIBRARY + "/by-book/{bookId}", bookId).with(bearer(owner.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userBookId.toString()));
    }

    @Test
    void deleteLibraryEntry() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);

        mockMvc.perform(delete(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId).with(bearer(owner.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId).with(bearer(owner.accessToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAnotherUsersLibraryEntry() throws Exception {
        UUID userBookId = addToLibrary(owner, bookId);
        AuthSession otherUser = registerUser();

        mockMvc.perform(get(ApiPaths.LIBRARY + USER_BOOK_ID_PATH, userBookId).with(bearer(otherUser.accessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value("Library entry not found"));
    }
}
