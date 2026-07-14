package com.whatiread.shelf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shelf.api.ShelfEventDto;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfEvent;
import com.whatiread.shelf.domain.ShelfEventType;
import com.whatiread.shelf.repository.ShelfEventRepository;
import com.whatiread.shared.util.DisplayNames;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
    private final UserLookupService userLookupService;
    private final ObjectMapper objectMapper;

    public ShelfEventService(
            ShelfEventRepository shelfEventRepository,
            UserLookupService userLookupService,
            ObjectMapper objectMapper
    ) {
        this.shelfEventRepository = shelfEventRepository;
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
