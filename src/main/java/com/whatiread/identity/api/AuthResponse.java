package com.whatiread.identity.api;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
