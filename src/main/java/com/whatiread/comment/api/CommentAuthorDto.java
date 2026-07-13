package com.whatiread.comment.api;

import java.util.UUID;

public record CommentAuthorDto(
        UUID id,
        String displayName,
        String avatarUrl
) {
}
