package com.whatiread.identity.suggest;

import java.util.UUID;

public record AdminUserSuggestDto(
        UUID id,
        String username,
        String displayName,
        String email
) {
}
