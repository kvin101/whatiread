package com.whatiread.comment.service.strategy;

import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.comment.domain.CommentTargetType;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.service.ShelfService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookCommentTargetAccessStrategy implements CommentTargetAccessStrategy {

    private final BookPersistencePort bookPersistencePort;
    private final LibraryService libraryService;
    private final ShelfService shelfService;

    public BookCommentTargetAccessStrategy(
            BookPersistencePort bookPersistencePort,
            LibraryService libraryService,
            ShelfService shelfService
    ) {
        this.bookPersistencePort = bookPersistencePort;
        this.libraryService = libraryService;
        this.shelfService = shelfService;
    }

    @Override
    public CommentTargetType targetType() {
        return CommentTargetType.BOOK;
    }

    @Override
    public boolean canView(UUID targetId, UUID viewerId) {
        if (!bookPersistencePort.existsById(targetId)) {
            return false;
        }
        if (libraryService.hasBook(viewerId, targetId)) {
            return true;
        }
        return shelfService.canViewBookViaShelf(targetId, viewerId);
    }

    @Override
    public UUID resolveOwnerId(UUID targetId) {
        return null;
    }
}
