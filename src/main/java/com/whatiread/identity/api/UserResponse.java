package com.whatiread.identity.api;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String phoneNumber,
        String avatarUrl,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean writer,
        String writerBio,
        boolean acceptRecommendations,
        Instant createdAt,
        boolean admin
) {
}
