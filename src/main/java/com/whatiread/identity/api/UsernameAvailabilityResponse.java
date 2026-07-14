package com.whatiread.identity.api;

public record UsernameAvailabilityResponse(
        String username,
        boolean valid,
        boolean available,
        String message
) {
}
