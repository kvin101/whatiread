package com.whatiread.messaging.websocket;

import com.whatiread.identity.security.JwtService;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shared.security.SecurityConstants;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserLookupService userLookupService;

    public StompJwtChannelInterceptor(JwtService jwtService, UserLookupService userLookupService) {
        this.jwtService = jwtService;
        this.userLookupService = userLookupService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authorization.substring(SecurityConstants.BEARER_PREFIX_LENGTH);
        JwtService.JwtClaims claims = jwtService.parse(token);
        if (!SecurityConstants.JWT_TYPE_ACCESS.equals(claims.type())) {
            throw new IllegalArgumentException("Access token required for WebSocket connection");
        }
        if (!userLookupService.isActiveAccount(claims.userId(), claims.tokenVersion())) {
            throw new IllegalArgumentException("User account is disabled or not found");
        }
        accessor.setUser(new StompPrincipal(claims.userId()));
        return message;
    }
}
