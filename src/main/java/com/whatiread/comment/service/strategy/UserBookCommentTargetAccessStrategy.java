package com.whatiread.comment.service.strategy;

import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.service.ShelfService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserBookCommentTargetAccessStrategy implements CommentTargetAccessStrategy {

    private final LibraryService libraryService;
    private final ShelfService shelfService;

    public UserBookCommentTargetAccessStrategy(LibraryService libraryService, ShelfService shelfService) {
        this.libraryService = libraryService;
        this.shelfService = shelfService;
    }

    @Override
    public CommentTargetType targetType() {
        return CommentTargetType.USER_BOOK;
    }

    @Override
    public boolean canView(UUID targetId, UUID viewerId) {
        UUID ownerId = libraryService.getOwnerId(targetId);
        if (ownerId.equals(viewerId)) {
            return true;
        }
        return shelfService.canViewUserBookViaShelf(targetId, viewerId);
    }

    @Override
    public UUID resolveOwnerId(UUID targetId) {
        return libraryService.getOwnerId(targetId);
    }
}
