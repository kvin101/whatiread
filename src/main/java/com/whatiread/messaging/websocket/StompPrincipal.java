package com.whatiread.messaging.websocket;

import java.security.Principal;
import java.util.UUID;

public record StompPrincipal(UUID userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
