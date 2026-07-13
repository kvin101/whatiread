package com.whatiread.instance.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class SetupIntegrationTest extends AbstractApiIntegrationTest {


    private static final String JSON_PATH_SETUP_REQUIRED = "$.setupRequired";
    private static final String REQUIRED_PATH = "/required";
    private static final String ADMIN_PATH = "/admin";

    @Test
    void setupFlow() throws Exception {
        MvcResult requiredResult = mockMvc.perform(get(ApiPaths.SETUP + REQUIRED_PATH))
                .andExpect(status().isOk())
                .andReturn();

        boolean needsSetup = JsonPath.read(requiredResult.getResponse().getContentAsString(), JSON_PATH_SETUP_REQUIRED);

        String body = """
                {
                  "email": "admin@example.com",
                  "password": "%s",
                  "firstName": "Admin",
                  "registrationEnabled": false
                }
                """.formatted(DEFAULT_PASSWORD);

        if (needsSetup) {
            mockMvc.perform(post(ApiPaths.SETUP + ADMIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(ApiPaths.SETUP + REQUIRED_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_SETUP_REQUIRED).value(false));
        } else {
            mockMvc.perform(post(ApiPaths.SETUP + ADMIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }
}
