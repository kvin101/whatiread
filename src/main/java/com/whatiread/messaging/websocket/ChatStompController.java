package com.whatiread.messaging.websocket;

import com.whatiread.messaging.api.ChatSendPayload;
import com.whatiread.messaging.api.ChatTypingPayload;
import com.whatiread.messaging.api.MessageDto;
import com.whatiread.messaging.service.MessagingService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ChatStompController {

    private final MessagingService messagingService;

    public ChatStompController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @MessageMapping("/chat.send")
    public MessageDto send(@Valid @Payload ChatSendPayload payload, Principal principal) {
        StompPrincipal user = (StompPrincipal) principal;
        return messagingService.sendMessage(
                user.userId(),
                payload.conversationId(),
                payload.body(),
                payload.mentions()
        );
    }

    @MessageMapping("/chat.typing")
    public void typing(@Valid @Payload ChatTypingPayload payload, Principal principal) {
        StompPrincipal user = (StompPrincipal) principal;
        messagingService.publishTyping(user.userId(), payload.conversationId(), payload.typing());
    }
}
