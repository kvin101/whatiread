package com.whatiread.identity.api;

import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String displayName,
        String avatarUrl,
        String firstName,
        String lastName
) {
}
