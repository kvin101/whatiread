package com.whatiread.catalog.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BookApiIntegrationTest extends AbstractApiIntegrationTest {

    private static final String BOOK_ID_PATH = "/{bookId}";
    private static final String BOOK_TITLE_LEFT_HAND = "The Left Hand of Darkness";
    private static final String BOOK_NOT_FOUND = "Book not found";
    private static final String AUTHOR_URSULA_K_LE_GUIN = "Ursula K. Le Guin";
    private static final String AUTHOR_WINSTON_STARR = "Winston Starr";
    private static final String AUTHOR_FRANK_HERBERT = "Frank Herbert";
    private static final String BOOK_SOURCE_MANUAL = "MANUAL";
    private static final String JSON_PATH_TITLE = "$.title";
    private static final String JSON_PATH_DETAIL = "$.detail";
    private static final String JSON_PATH_AUTHORS = "$.authors";
    private static final String JSON_PATH_AUTHORS_0 = "$.authors[0]";
    private static final String JSON_PATH_PAGE_COUNT = "$.pageCount";
    private static final String JSON_PATH_SOURCE = "$.source";
    private static final String JSON_PATH_CONTENT = "$.content";
    private static final String MISSING_BOOK_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void createBookRequiresAuthentication() throws Exception {
        mockMvc.perform(post(ApiPaths.BOOKS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Dune",
                                  "authors": ["%s"],
                                  "pageCount": 688
                                }
                                """.formatted(AUTHOR_FRANK_HERBERT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAndGetBook() throws Exception {
        AuthSession session = registerUser();

        String createResponse = mockMvc.perform(post(ApiPaths.BOOKS)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "subtitle": "A Novel",
                                  "authors": ["%s"],
                                  "pageCount": 304,
                                  "description": "A science fiction classic."
                                }
                                """.formatted(BOOK_TITLE_LEFT_HAND, AUTHOR_URSULA_K_LE_GUIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_TITLE).value(BOOK_TITLE_LEFT_HAND))
                .andExpect(jsonPath(JSON_PATH_AUTHORS, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_AUTHORS_0).value(AUTHOR_URSULA_K_LE_GUIN))
                .andExpect(jsonPath(JSON_PATH_PAGE_COUNT).value(304))
                .andExpect(jsonPath(JSON_PATH_SOURCE).value(BOOK_SOURCE_MANUAL))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bookId = com.jayway.jsonpath.JsonPath.read(createResponse, JSON_PATH_ID);

        mockMvc.perform(get(ApiPaths.BOOKS + BOOK_ID_PATH, bookId).with(bearer(session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID).value(bookId))
                .andExpect(jsonPath(JSON_PATH_TITLE).value(BOOK_TITLE_LEFT_HAND));
    }

    @Test
    void createDuplicateBookReturnsSameCatalogEntry() throws Exception {
        AuthSession session = registerUser();

        String payload = """
                {
                  "title": "Harry Porter Trivia",
                  "authors": ["%s"],
                  "coverUrl": "https://covers.openlibrary.org/b/id/13217155-M.jpg",
                  "externalId": "/works/OL123W",
                  "source": "OPEN_LIBRARY"
                }
                """.formatted(AUTHOR_WINSTON_STARR);

        String first = mockMvc.perform(post(ApiPaths.BOOKS)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstId = com.jayway.jsonpath.JsonPath.read(first, JSON_PATH_ID);

        mockMvc.perform(post(ApiPaths.BOOKS)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_ID).value(firstId));
    }

    @Test
    void getMissingBookReturnsNotFound() throws Exception {
        AuthSession session = registerUser();

        mockMvc.perform(get(ApiPaths.BOOKS + BOOK_ID_PATH, MISSING_BOOK_ID)
                        .with(bearer(session.accessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value(BOOK_NOT_FOUND));
    }

    @Test
    void bookSearchIsPublic() throws Exception {
        mockMvc.perform(get(ApiPaths.BOOKS_SEARCH).param("q", "dune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTENT).isArray());
    }
}
