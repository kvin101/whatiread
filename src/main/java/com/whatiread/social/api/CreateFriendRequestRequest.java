package com.whatiread.social.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import java.util.UUID;

public record CreateFriendRequestRequest(
        UUID userId,
        @Email String email
) {

    @AssertTrue(message = "Either userId or email is required")
    public boolean hasTarget() {
        return userId != null || (email != null && !email.isBlank());
    }
}
