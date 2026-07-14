package com.whatiread.identity.suggest;

import java.util.UUID;

public record UserSuggestDto(
        UUID id,
        String username,
        String displayName
) {
}
