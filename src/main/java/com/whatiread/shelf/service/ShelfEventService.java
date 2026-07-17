package com.whatiread.shelf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shelf.api.ShelfEventDto;
import com.whatiread.shelf.api.ShelfFeedEventDto;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfEvent;
import com.whatiread.shelf.domain.ShelfEventType;
import com.whatiread.shelf.repository.ShelfEventRepository;
import com.whatiread.shelf.repository.ShelfRepository;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shared.util.DisplayNames;
import com.whatiread.shared.util.KeysetCursor;
import com.whatiread.social.service.FriendshipService;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShelfEventService {

    static final Collection<ShelfEventType> BOOK_CHANGE_TYPES = List.of(
            ShelfEventType.BOOK_ADDED,
            ShelfEventType.BOOK_REMOVED
    );
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };
    private final ShelfEventRepository shelfEventRepository;
    private final ShelfRepository shelfRepository;
    private final FriendshipService friendshipService;
    private final UserLookupService userLookupService;
    private final ObjectMapper objectMapper;

    public ShelfEventService(
            ShelfEventRepository shelfEventRepository,
            ShelfRepository shelfRepository,
            FriendshipService friendshipService,
            UserLookupService userLookupService,
            ObjectMapper objectMapper
    ) {
        this.shelfEventRepository = shelfEventRepository;
        this.shelfRepository = shelfRepository;
        this.friendshipService = friendshipService;
        this.userLookupService = userLookupService;
        this.objectMapper = objectMapper;
    }

    private String serializePayload(Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid event payload", ex);
        }
    }

    private Map<String, String> deserializePayload(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }

    public void record(Shelf shelf, UUID actorId, ShelfEventType type, Map<String, String> payload) {
        User actor = userLookupService.getPersistenceReference(actorId);
        String json = serializePayload(payload);
        shelfEventRepository.save(new ShelfEvent(shelf, actor, type, json));
    }

    @Transactional(readOnly = true)
    public Page<ShelfEventDto> list(UUID shelfId, Pageable pageable) {
        return shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ShelfEventDto> listByTypes(UUID shelfId, Collection<ShelfEventType> types, Pageable pageable) {
        return shelfEventRepository.findByShelf_IdAndEventTypeInOrderByCreatedAtDesc(shelfId, types, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CursorPage<ShelfFeedEventDto> listFriendFeed(UUID viewerId, String cursor, int limit) {
        List<UUID> shelfIds = listVisibleFriendShelfIds(viewerId);
        if (shelfIds.isEmpty()) {
            return new CursorPage<>(List.of(), null, false);
        }
        int pageSize = Math.min(Math.max(limit, 1), 100);
        KeysetCursor.Parts position = KeysetCursor.decode(cursor);
        List<ShelfEvent> rows;
        if (position.updatedAt() == null) {
            rows = shelfEventRepository.findFirstKeysetPageByShelfIds(
                    shelfIds,
                    PageRequest.of(0, pageSize + 1)
            );
        } else {
            rows = shelfEventRepository.findKeysetPageByShelfIds(
                    shelfIds,
                    position.updatedAt(),
                    position.id(),
                    PageRequest.of(0, pageSize + 1)
            );
        }
        boolean hasMore = rows.size() > pageSize;
        List<ShelfEvent> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<ShelfFeedEventDto> items = pageRows.stream().map(this::toFeedDto).toList();
        String nextCursor = hasMore && !pageRows.isEmpty()
                ? KeysetCursor.encode(pageRows.getLast().getCreatedAt(), pageRows.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor, hasMore);
    }

    private ShelfFeedEventDto toFeedDto(ShelfEvent event) {
        Shelf shelf = event.getShelf();
        return new ShelfFeedEventDto(
                event.getId(),
                event.getEventType(),
                event.getActor().getId(),
                DisplayNames.format(event.getActor()),
                shelf.getId(),
                shelf.getName(),
                shelf.getOwner().getId(),
                DisplayNames.format(shelf.getOwner()),
                deserializePayload(event.getPayload()),
                event.getCreatedAt()
        );
    }

    private List<UUID> listVisibleFriendShelfIds(UUID viewerId) {
        List<UUID> friendIds = friendshipService.listFriendIds(viewerId);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        return shelfRepository.findVisibleFriendShelfIds(friendIds);
    }

    private ShelfEventDto toDto(ShelfEvent event) {
        User actor = event.getActor();
        return new ShelfEventDto(
                event.getId(),
                event.getEventType(),
                actor.getId(),
                DisplayNames.format(actor),
                deserializePayload(event.getPayload()),
                event.getCreatedAt()
        );
    }
}
