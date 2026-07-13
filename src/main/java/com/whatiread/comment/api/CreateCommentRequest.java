package com.whatiread.comment.api;

import com.whatiread.comment.domain.CommentTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCommentRequest(
        @NotNull CommentTargetType targetType,
        @NotNull UUID targetId,
        @NotBlank @Size(max = 2000) String body
) {
}
