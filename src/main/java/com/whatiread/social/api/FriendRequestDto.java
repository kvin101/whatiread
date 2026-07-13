package com.whatiread.social.api;

import com.whatiread.social.domain.FriendRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record FriendRequestDto(
        UUID id,
        FriendUserDto requester,
        FriendUserDto addressee,
        FriendRequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
