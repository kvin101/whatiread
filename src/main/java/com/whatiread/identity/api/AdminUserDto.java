package com.whatiread.identity.api;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        boolean admin,
        boolean enabled,
        Instant createdAt
) {
}
