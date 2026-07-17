package com.whatiread.activity.service;

import com.whatiread.activity.api.ActivityItemDto;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shelf.api.ShelfFeedEventDto;
import com.whatiread.shelf.service.ShelfEventService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivityFeedService {

    private final ShelfEventService shelfEventService;

    public ActivityFeedService(ShelfEventService shelfEventService) {
        this.shelfEventService = shelfEventService;
    }

    public CursorPage<ActivityItemDto> list(UUID viewerId, String cursor, int limit) {
        CursorPage<ShelfFeedEventDto> page = shelfEventService.listFriendFeed(viewerId, cursor, limit);
        return new CursorPage<>(
                page.items().stream().map(this::toActivityItem).toList(),
                page.nextCursor(),
                page.hasMore()
        );
    }

    private ActivityItemDto toActivityItem(ShelfFeedEventDto event) {
        return new ActivityItemDto(
                event.id(),
                event.eventType(),
                event.actorId(),
                event.actorDisplayName(),
                event.shelfId(),
                event.shelfName(),
                event.shelfOwnerId(),
                event.shelfOwnerDisplayName(),
                event.payload(),
                event.createdAt()
        );
    }
}
