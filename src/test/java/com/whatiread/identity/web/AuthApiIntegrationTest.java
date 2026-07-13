package com.whatiread.identity.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiIntegrationTest extends AbstractApiIntegrationTest {

    private static final String JSON_PATH_DETAIL = "$.detail";
    private static final String JSON_PATH_USER_FIRST_NAME = "$.user.firstName";
    private static final String JSON_PATH_USER_LAST_NAME = "$.user.lastName";
    private static final String JSON_PATH_EMAIL = "$.email";
    private static final String REGISTER_PATH = "/register";
    private static final String REFRESH_PATH = "/refresh";
    private static final String LOGIN_PATH = "/login";
    private static final String LOGOUT_PATH = "/logout";
    private static final String WRONG_PASSWORD = "wrong-password";

    @Test
    void registerCreatesUserAndReturnsTokens() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post(ApiPaths.AUTH + REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "firstName": "Ada",
                                  "lastName": "Lovelace"
                                }
                                """.formatted(email, DEFAULT_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_ACCESS_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_REFRESH_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_USER_EMAIL).value(email))
                .andExpect(jsonPath(JSON_PATH_USER_FIRST_NAME).value("Ada"))
                .andExpect(jsonPath(JSON_PATH_USER_LAST_NAME).value("Lovelace"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        registerUser(email, DEFAULT_PASSWORD, "First", "User");

        mockMvc.perform(post(ApiPaths.AUTH + REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "firstName": "Second",
                                  "lastName": "User"
                                }
                                """.formatted(email, DEFAULT_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value("Email already registered"));
    }

    @Test
    void loginReturnsTokensForValidCredentials() throws Exception {
        String email = uniqueEmail();
        registerUser(email, DEFAULT_PASSWORD, "Alan", "Turing");

        mockMvc.perform(post(ApiPaths.AUTH + LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ACCESS_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_REFRESH_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_USER_EMAIL).value(email));
    }

    @Test
    void loginRejectsInvalidPassword() throws Exception {
        String email = uniqueEmail();
        registerUser(email, DEFAULT_PASSWORD, "Grace", "Hopper");

        mockMvc.perform(post(ApiPaths.AUTH + LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value(containsString("credentials")));
    }

    @Test
    void refreshIssuesNewTokens() throws Exception {
        AuthSession session = registerUser();

        mockMvc.perform(post(ApiPaths.AUTH + REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ACCESS_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_REFRESH_TOKEN).isNotEmpty())
                .andExpect(jsonPath(JSON_PATH_USER_ID).value(session.userId().toString()));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        AuthSession session = registerUser();

        mockMvc.perform(post(ApiPaths.AUTH + LOGOUT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(ApiPaths.AUTH + REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_PATH_DETAIL).value("Invalid refresh token"));
    }

    @Test
    void meReturnsCurrentUserWhenAuthenticated() throws Exception {
        AuthSession session = registerUser();

        mockMvc.perform(get(ApiPaths.ME).with(bearer(session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID).value(session.userId().toString()))
                .andExpect(jsonPath(JSON_PATH_EMAIL).value(session.email()));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get(ApiPaths.ME))
                .andExpect(status().isForbidden());
    }
}
