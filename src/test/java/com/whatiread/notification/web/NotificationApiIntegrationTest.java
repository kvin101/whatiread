package com.whatiread.notification.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class NotificationApiIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    void friendRequestCreatesNotification() throws Exception {
        AuthSession requester = registerUser();
        AuthSession recipient = registerUser();

        mockMvc.perform(post(ApiPaths.FRIENDS + "/requests")
                        .with(bearer(requester.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(recipient.userId())))
                .andExpect(status().isCreated());

        String notifications = mockMvc.perform(get(ApiPaths.NOTIFICATIONS).with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("FRIEND_REQUEST"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String notificationId = JsonPath.read(notifications, "$[0].id");

        mockMvc.perform(post(ApiPaths.NOTIFICATIONS + "/{id}/read", notificationId)
                        .with(bearer(recipient.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post(ApiPaths.NOTIFICATIONS + "/read-all").with(bearer(recipient.accessToken())))
                .andExpect(status().isNoContent());
    }
}
