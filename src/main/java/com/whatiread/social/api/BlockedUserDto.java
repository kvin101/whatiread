package com.whatiread.social.api;

import java.time.Instant;
import java.util.UUID;

public record BlockedUserDto(
        UUID id,
        String firstName,
        String lastName,
        String displayName,
        String avatarUrl,
        Instant blockedAt
) {
}
