package com.whatiread.comment.service.strategy;

import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.shelf.service.ShelfService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShelfCommentTargetAccessStrategy implements CommentTargetAccessStrategy {

    private final ShelfService shelfService;

    public ShelfCommentTargetAccessStrategy(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @Override
    public CommentTargetType targetType() {
        return CommentTargetType.SHELF;
    }

    @Override
    public boolean canView(UUID targetId, UUID viewerId) {
        return shelfService.canViewShelf(targetId, viewerId);
    }

    @Override
    public UUID resolveOwnerId(UUID targetId) {
        return shelfService.getShelfOwnerId(targetId);
    }
}
