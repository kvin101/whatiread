package com.whatiread.identity.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class InstanceAdminMeIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private InstanceSettingsService instanceSettingsService;

    @Test
    void meReturnsAdminTrueForInstanceAdmin() throws Exception {
        AuthSession admin = registerUser();
        instanceSettingsService.setAdminUserId(admin.userId());

        mockMvc.perform(get(ApiPaths.ME).with(bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.userId().toString()))
                .andExpect(jsonPath("$.admin").value(true));
    }

    @Test
    void registerFirstUserReturnsAdminTrue() throws Exception {
        Assumptions.assumeTrue(instanceSettingsService.isSetupRequired());

        String email = uniqueEmail();
        String response = mockMvc.perform(post(ApiPaths.AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "firstName": "First",
                                  "lastName": "Admin"
                                }
                                """.formatted(email, DEFAULT_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.admin").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = JsonPath.read(response, JSON_PATH_ACCESS_TOKEN);

        mockMvc.perform(get(ApiPaths.ME).with(bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.admin").value(true));
    }
}
