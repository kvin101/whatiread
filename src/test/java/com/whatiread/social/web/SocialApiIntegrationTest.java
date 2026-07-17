package com.whatiread.social.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SocialApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String REQUESTS = "/requests";
    private static final String STATUS = "$.status";
    private static final String V_0_ID = "$[0].id";
    private static final String ID = "$.id";
    private static final String BLOCKEDUSERID_BLOCK = "/{blockedUserId}/block";
    private static final String REQUESTS_INCOMING = "/requests/incoming";
    private static final String BLOCKED = "/blocked";

    @Test
    void friendRequestLifecycle() throws Exception {
        AuthSession alice = registerUser();
        AuthSession bob = registerUser();

        String requestResponse = mockMvc.perform(post(ApiPaths.FRIENDS + REQUESTS)
                        .with(bearer(alice.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(bob.userId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(STATUS).value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID requestId = UUID.fromString(JsonPath.read(requestResponse, ID));

        mockMvc.perform(get(ApiPaths.FRIENDS + "/requests/outgoing").with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath(V_0_ID).value(requestId.toString()));

        mockMvc.perform(get(ApiPaths.FRIENDS + REQUESTS_INCOMING).with(bearer(bob.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post(ApiPaths.FRIENDS + "/requests/{requestId}/accept", requestId)
                        .with(bearer(bob.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("ACCEPTED"));

        mockMvc.perform(get(ApiPaths.FRIENDS).with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath(V_0_ID).value(bob.userId().toString()));

        mockMvc.perform(delete(ApiPaths.FRIENDS + "/{friendUserId}", bob.userId())
                        .with(bearer(alice.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.FRIENDS).with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void declineAndCancelFriendRequests() throws Exception {
        AuthSession sender = registerUser();
        AuthSession recipient = registerUser();

        String created = mockMvc.perform(post(ApiPaths.FRIENDS + REQUESTS)
                        .with(bearer(sender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(recipient.email())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID requestId = UUID.fromString(JsonPath.read(created, ID));

        mockMvc.perform(post(ApiPaths.FRIENDS + "/requests/{requestId}/decline", requestId)
                        .with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("REJECTED"));

        String second = mockMvc.perform(post(ApiPaths.FRIENDS + REQUESTS)
                        .with(bearer(sender.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(recipient.userId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID secondId = UUID.fromString(JsonPath.read(second, ID));

        mockMvc.perform(delete(ApiPaths.FRIENDS + "/requests/{requestId}", secondId)
                        .with(bearer(sender.accessToken())))
                .andExpect(status().isNoContent());

        String incoming = mockMvc.perform(get(ApiPaths.FRIENDS + REQUESTS_INCOMING)
                        .with(bearer(recipient.accessToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<?> pending = JsonPath.read(incoming, "$[?(@.status=='PENDING')]");
        org.junit.jupiter.api.Assertions.assertTrue(pending.isEmpty());
    }

    @Test
    void blockUnfriendAndPreventRequests() throws Exception {
        AuthSession blocker = registerUser();
        AuthSession target = registerUser();
        makeFriends(blocker, target);

        mockMvc.perform(post(ApiPaths.FRIENDS + BLOCKEDUSERID_BLOCK, target.userId())
                        .with(bearer(blocker.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.FRIENDS).with(bearer(blocker.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(ApiPaths.FRIENDS + BLOCKED).with(bearer(blocker.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath(V_0_ID).value(target.userId().toString()));

        mockMvc.perform(post(ApiPaths.FRIENDS + REQUESTS)
                        .with(bearer(target.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(blocker.userId())))
                .andExpect(status().isConflict());

        mockMvc.perform(delete(ApiPaths.FRIENDS + BLOCKEDUSERID_BLOCK, target.userId())
                        .with(bearer(blocker.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ApiPaths.FRIENDS + BLOCKED).with(bearer(blocker.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(ApiPaths.FRIENDS).with(bearer(blocker.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath(V_0_ID).value(target.userId().toString()));
    }
}
