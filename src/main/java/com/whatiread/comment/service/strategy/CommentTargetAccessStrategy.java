package com.whatiread.comment.service.strategy;

import com.whatiread.comment.domain.CommentTargetType;
import java.util.UUID;

public interface CommentTargetAccessStrategy {

    CommentTargetType targetType();

    boolean canView(UUID targetId, UUID viewerId);

    UUID resolveOwnerId(UUID targetId);
}
