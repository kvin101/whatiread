package com.whatiread.social.api;

import java.util.UUID;

public record FriendUserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String avatarUrl
) {
}
