package com.whatiread.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.shared.security.SecurityConstants;
import com.whatiread.shared.web.ApiPaths;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractApiIntegrationTest {

    protected static final String DEFAULT_PASSWORD = TestConstants.TEST_PASSWORD;
    protected static final String JSON_PATH_ID = "$.id";
    protected static final String JSON_PATH_ACCESS_TOKEN = "$.accessToken";
    protected static final String JSON_PATH_REFRESH_TOKEN = "$.refreshToken";
    protected static final String JSON_PATH_USER_EMAIL = "$.user.email";
    protected static final String JSON_PATH_USER_ID = "$.user.id";

    @Autowired
    protected MockMvc mockMvc;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected AuthSession registerUser() throws Exception {
        return registerUser(uniqueEmail(), DEFAULT_PASSWORD, "Jane", "Doe");
    }

    protected AuthSession registerUser(String email, String password, String firstName, String lastName)
            throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "%s",
                  "lastName": "%s"
                }
                """.formatted(email, password, firstName, lastName);

        String response = mockMvc.perform(post(ApiPaths.AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new AuthSession(
                JsonPath.read(response, JSON_PATH_ACCESS_TOKEN),
                JsonPath.read(response, JSON_PATH_REFRESH_TOKEN),
                UUID.fromString(JsonPath.read(response, JSON_PATH_USER_ID)),
                JsonPath.read(response, JSON_PATH_USER_EMAIL)
        );
    }

    protected RequestPostProcessor bearer(String accessToken) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, SecurityConstants.bearerToken(accessToken));
            return request;
        };
    }

    protected AuthSession login(String email, String password) throws Exception {
        String response = mockMvc.perform(post(ApiPaths.AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new AuthSession(
                JsonPath.read(response, JSON_PATH_ACCESS_TOKEN),
                JsonPath.read(response, JSON_PATH_REFRESH_TOKEN),
                UUID.fromString(JsonPath.read(response, JSON_PATH_USER_ID)),
                JsonPath.read(response, JSON_PATH_USER_EMAIL)
        );
    }

    protected UUID createBook(AuthSession session, String title, String author, int pageCount) throws Exception {
        String response = mockMvc.perform(post(ApiPaths.BOOKS)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "authors": ["%s"],
                                  "pageCount": %d
                                }
                                """.formatted(title, author, pageCount)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(JsonPath.read(response, JSON_PATH_ID));
    }

    protected UUID addToLibrary(AuthSession session, UUID bookId) throws Exception {
        String response = mockMvc.perform(post(ApiPaths.LIBRARY)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": "%s"
                                }
                                """.formatted(bookId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(JsonPath.read(response, JSON_PATH_ID));
    }

    protected UUID createShelf(AuthSession session, String name, String visibility) throws Exception {
        String response = mockMvc.perform(post(ApiPaths.SHELVES)
                        .with(bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "visibility": "%s"
                                }
                                """.formatted(name, visibility)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(JsonPath.read(response, JSON_PATH_ID));
    }

    protected void makeFriends(AuthSession requester, AuthSession target) throws Exception {
        mockMvc.perform(post(ApiPaths.FRIENDS + "/requests")
                        .with(bearer(requester.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(target.userId())))
                .andExpect(status().isCreated());

        String incoming = mockMvc.perform(get(ApiPaths.FRIENDS + "/requests/incoming")
                        .with(bearer(target.accessToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> requestIds = JsonPath.read(incoming, "$[*].id");
        UUID requestId = UUID.fromString(requestIds.getFirst());

        mockMvc.perform(post(ApiPaths.FRIENDS + "/requests/{requestId}/accept", requestId)
                        .with(bearer(target.accessToken())))
                .andExpect(status().isOk());
    }

    protected record AuthSession(String accessToken, String refreshToken, UUID userId, String email) {
    }
}
