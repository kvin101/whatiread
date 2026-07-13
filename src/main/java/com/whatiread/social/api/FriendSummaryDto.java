package com.whatiread.social.api;

import java.time.Instant;
import java.util.UUID;

public record FriendSummaryDto(
        UUID id,
        String firstName,
        String lastName,
        String displayName,
        String avatarUrl,
        Instant friendsSince
) {
}
