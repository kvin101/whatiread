package com.whatiread.instance.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AdminApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String REGISTRATION = "/registration";
    @Autowired
    private InstanceSettingsService instanceSettingsService;

    private AuthSession admin;

    @BeforeEach
    void setUpAdmin() throws Exception {
        admin = registerUser();
        instanceSettingsService.setAdminUserId(admin.userId());
    }

    @Test
    void adminCanToggleRegistration() throws Exception {
        mockMvc.perform(patch(ApiPaths.ADMIN_INSTANCE + REGISTRATION)
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.SETUP + "/required"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationEnabled").value(false));

        mockMvc.perform(patch(ApiPaths.ADMIN_INSTANCE + REGISTRATION)
                        .with(bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotToggleRegistration() throws Exception {
        AuthSession regularUser = registerUser();

        mockMvc.perform(patch(ApiPaths.ADMIN_INSTANCE + REGISTRATION)
                        .with(bearer(regularUser.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
