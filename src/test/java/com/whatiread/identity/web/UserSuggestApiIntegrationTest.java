package com.whatiread.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;

class UserSuggestApiIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void userSuggestRequiresAuthentication() throws Exception {
        mockMvc.perform(get(ApiPaths.USERS_SUGGEST).param("q", "jane"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userSuggestRequiresQuery() throws Exception {
        AuthSession session = registerUser();

        mockMvc.perform(get(ApiPaths.USERS_SUGGEST).with(bearer(session.accessToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userSuggestReturnsArrayForAuthenticatedUser() throws Exception {
        AuthSession session = registerUser();

        mockMvc.perform(get(ApiPaths.USERS_SUGGEST)
                        .param("q", "jane")
                        .param("scope", "invite")
                        .with(bearer(session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void userSuggestFriendsScopeReturnsArray() throws Exception {
        AuthSession alice = registerUser();
        registerUser();

        mockMvc.perform(get(ApiPaths.USERS_SUGGEST)
                        .param("q", "jane")
                        .param("scope", "friends")
                        .with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
