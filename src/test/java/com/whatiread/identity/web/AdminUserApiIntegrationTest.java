package com.whatiread.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AdminUserApiIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private InstanceSettingsService instanceSettingsService;

    private AuthSession admin;
    private String newUserEmail;

    @BeforeEach
    void setUpAdmin() throws Exception {
        admin = registerUser();
        instanceSettingsService.setAdminUserId(admin.userId());
        newUserEmail = uniqueEmail();
    }

    @Test
    void adminCanCreateListBanResetAndDeleteUser() throws Exception {
        String createResponse = mockMvc.perform(post(ApiPaths.ADMIN_USERS)
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "username": "%s",
                                  "password": "password123",
                                  "firstName": "Invited",
                                  "lastName": "Reader",
                                  "role": "USER"
                                }
                                """.formatted(newUserEmail, uniqueUsername())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(newUserEmail))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.admin").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID createdUserId = UUID.fromString(JsonPath.read(createResponse, "$.id"));

        mockMvc.perform(get(ApiPaths.ADMIN_USERS)
                        .param("q", "Invited")
                        .with(bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(newUserEmail));

        mockMvc.perform(patch(ApiPaths.ADMIN_USERS + "/" + createdUserId + "/enabled")
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post(ApiPaths.AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(newUserEmail)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch(ApiPaths.ADMIN_USERS + "/" + createdUserId + "/enabled")
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(patch(ApiPaths.ADMIN_USERS + "/" + createdUserId + "/password")
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "newpassword99"
                                }
                                """))
                .andExpect(status().isOk());

        AuthSession invited = login(newUserEmail, "newpassword99");
        mockMvc.perform(get(ApiPaths.ME).with(bearer(invited.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newUserEmail));

        mockMvc.perform(delete(ApiPaths.ADMIN_USERS + "/" + createdUserId)
                        .with(bearer(admin.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.ADMIN_USERS)
                        .param("q", newUserEmail)
                        .with(bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listUsersWithoutQueryReturnsAllUsers() throws Exception {
        mockMvc.perform(get(ApiPaths.ADMIN_USERS).with(bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.email == '%s')]".formatted(admin.email())).isNotEmpty());
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        AuthSession regularUser = registerUser();

        mockMvc.perform(get(ApiPaths.ADMIN_USERS).with(bearer(regularUser.accessToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ApiPaths.ADMIN_USERS)
                        .with(bearer(regularUser.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "username": "%s",
                                  "password": "password123",
                                  "firstName": "Nope",
                                  "role": "USER"
                                }
                                """.formatted(uniqueEmail(), uniqueUsername())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotDeleteSelf() throws Exception {
        mockMvc.perform(delete(ApiPaths.ADMIN_USERS + "/" + admin.userId())
                        .with(bearer(admin.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetPasswordInvalidatesExistingSession() throws Exception {
        String createResponse = mockMvc.perform(post(ApiPaths.ADMIN_USERS)
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "username": "%s",
                                  "password": "password123",
                                  "firstName": "Session",
                                  "lastName": "Victim",
                                  "role": "USER"
                                }
                                """.formatted(newUserEmail, uniqueUsername())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID createdUserId = UUID.fromString(JsonPath.read(createResponse, "$.id"));
        AuthSession victim = login(newUserEmail, "password123");
        String oldAccessToken = victim.accessToken();
        String oldRefreshToken = victim.refreshToken();

        mockMvc.perform(patch(ApiPaths.ADMIN_USERS + "/" + createdUserId + "/password")
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "newpassword99"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.ME).with(bearer(oldAccessToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(ApiPaths.AUTH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(oldRefreshToken)))
                .andExpect(status().isUnauthorized());

        AuthSession renewed = login(newUserEmail, "newpassword99");
        mockMvc.perform(get(ApiPaths.ME).with(bearer(renewed.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newUserEmail));
    }
}
