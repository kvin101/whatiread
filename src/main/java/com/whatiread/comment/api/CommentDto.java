package com.whatiread.comment.api;

import com.whatiread.comment.domain.CommentTargetType;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id,
        CommentTargetType targetType,
        UUID targetId,
        CommentAuthorDto author,
        String body,
        Instant createdAt,
        Instant updatedAt
) {
}
