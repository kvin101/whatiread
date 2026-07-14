package com.whatiread.messaging.web;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.whatiread.messaging.service.MessagingService;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessagingApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String ID = "$.id";
    private static final String WITH_FRIENDUSERID = "/with/{friendUserId}";
    private static final String HELLO_FROM_ALICE = "Hello from Alice";
    @Autowired
    private MessagingService messagingService;

    private AuthSession alice;
    private AuthSession bob;

    @BeforeEach
    void setUp() throws Exception {
        alice = registerUser();
        bob = registerUser();
        makeFriends(alice, bob);
    }

    @Test
    void conversationAndMessages() throws Exception {
        String conversationResponse = mockMvc.perform(post(ApiPaths.CONVERSATIONS + WITH_FRIENDUSERID, bob.userId())
                        .with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ID).isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID conversationId = UUID.fromString(JsonPath.read(conversationResponse, ID));

        messagingService.sendMessage(alice.userId(), conversationId, HELLO_FROM_ALICE);

        mockMvc.perform(get(ApiPaths.CONVERSATIONS).with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(ApiPaths.CONVERSATIONS + "/{conversationId}/messages", conversationId)
                        .with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].body").value(HELLO_FROM_ALICE));

        mockMvc.perform(get(ApiPaths.CONVERSATIONS + "/unread-count").with(bearer(bob.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    void cannotSendMessageAfterUnfriending() throws Exception {
        String conversationResponse = mockMvc.perform(post(ApiPaths.CONVERSATIONS + WITH_FRIENDUSERID, bob.userId())
                        .with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID conversationId = UUID.fromString(JsonPath.read(conversationResponse, ID));

        mockMvc.perform(delete(ApiPaths.FRIENDS + "/{friendUserId}", bob.userId())
                        .with(bearer(alice.accessToken())))
                .andExpect(status().isNoContent());

        assertThrows(
                ForbiddenException.class, () ->
                        messagingService.sendMessage(alice.userId(), conversationId, "Should be blocked"));
    }

    @Test
    void groupConversationMessaging() throws Exception {
        AuthSession carol = registerUser();
        makeFriends(alice, carol);

        String groupResponse = mockMvc.perform(post(ApiPaths.CONVERSATIONS + "/groups")
                        .with(bearer(alice.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Book Club",
                                  "memberUserIds": ["%s", "%s"]
                                }
                                """.formatted(bob.userId(), carol.userId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.name").value("Book Club"))
                .andExpect(jsonPath("$.memberCount").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID groupId = UUID.fromString(JsonPath.read(groupResponse, ID));

        messagingService.sendMessage(alice.userId(), groupId, "Welcome everyone");

        mockMvc.perform(get(ApiPaths.CONVERSATIONS + "/{conversationId}/messages", groupId)
                        .with(bearer(bob.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].body").value("Welcome everyone"));

        mockMvc.perform(get(ApiPaths.CONVERSATIONS + "/groups").with(bearer(alice.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Book Club"));

        mockMvc.perform(post(ApiPaths.CONVERSATIONS + "/{conversationId}/leave", groupId)
                        .with(bearer(carol.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(put(ApiPaths.CONVERSATIONS + "/{conversationId}", groupId)
                        .with(bearer(alice.accessToken()))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renamed Club"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Club"));
    }
}
