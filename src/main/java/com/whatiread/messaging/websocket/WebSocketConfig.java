package com.whatiread.messaging.websocket;

import com.whatiread.config.WhatIReadProperties;
import com.whatiread.config.CorsOrigins;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
    private final WhatIReadProperties properties;

    public WebSocketConfig(
            StompJwtChannelInterceptor stompJwtChannelInterceptor,
            WhatIReadProperties properties
    ) {
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(splitOrigins())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }

    private String[] splitOrigins() {
        String origins = properties.cors() != null
                ? properties.cors().allowedOrigins()
                : "http://localhost:5173";
        return CorsOrigins.split(origins).toArray(String[]::new);
    }
}
