package com.whatiread.identity.api;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 20) String phoneNumber,
        @Size(max = 2048) String avatarUrl,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String country,
        Boolean writer,
        @Size(max = 2000) String writerBio,
        Boolean acceptRecommendations
) {
}
