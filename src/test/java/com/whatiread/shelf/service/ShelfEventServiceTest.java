package com.whatiread.shelf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfEvent;
import com.whatiread.shelf.domain.ShelfEventType;
import com.whatiread.shelf.repository.ShelfEventRepository;
import com.whatiread.support.TestConstants;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ShelfEventServiceTest {


    private static final String HASH = "hash";
    private static final String ADA = "Ada";
    private static final String ACTOR_EXAMPLE_COM = "actor@example.com";
    private static final String LOVELACE = "Lovelace";
    private static final String BOOK_TITLE_PAYLOAD = "{\"bookTitle\":\"Dune\"}";
    private static final String ADA_LOVELACE = "Ada Lovelace";
    private static final String SHELF_NAME_READING = "Reading";
    private static final String INVALID_JSON_PAYLOAD = "{not-json";
    @Mock
    private ShelfEventRepository shelfEventRepository;
    @Mock
    private UserLookupService userLookupService;

    @InjectMocks
    private ShelfEventService shelfEventService;

    private UUID userId;
    private UUID shelfId;
    private User owner;
    private Shelf shelf;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                Field createdAt = findField(type, "createdAt");
                if (createdAt != null) {
                    createdAt.setAccessible(true);
                    createdAt.set(entity, java.time.Instant.parse("2024-01-01T00:00:00Z"));
                }
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static Field findField(Class<?> type, String name) {
        while (type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        shelfId = UUID.randomUUID();
        owner = new User("owner@example.com", HASH, "Owner", "User");
        setId(owner, userId);
        shelf = new Shelf(owner, SHELF_NAME_READING, "reading");
        setId(shelf, shelfId);
    }

    @Test
    void recordPersistsShelfEvent() {
        when(userLookupService.getPersistenceReference(userId)).thenReturn(owner);

        shelfEventService.record(shelf, userId, ShelfEventType.SHELF_CREATED, Map.of("name", SHELF_NAME_READING));

        verify(shelfEventRepository).save(any(ShelfEvent.class));
    }

    @Test
    void listMapsStoredEvents() {
        ShelfEvent event = new ShelfEvent(shelf, owner, ShelfEventType.BOOK_ADDED, BOOK_TITLE_PAYLOAD);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test
    void listFormatsActorNameFromFirstAndLast() {
        User actor = new User(ACTOR_EXAMPLE_COM, HASH, ADA, LOVELACE);
        setId(actor, UUID.randomUUID());
        ShelfEvent event = new ShelfEvent(shelf, actor, ShelfEventType.SHELF_UPDATED, null);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent().getFirst().actorDisplayName())
                .isEqualTo(ADA_LOVELACE);
    }

    @Test
    void recordHandlesEmptyPayload() {
        when(userLookupService.getPersistenceReference(userId)).thenReturn(owner);

        shelfEventService.record(shelf, userId, ShelfEventType.SHELF_UPDATED, Map.of());

        verify(shelfEventRepository).save(any(ShelfEvent.class));
    }

    @Test
    void recordHandlesNullPayload() {
        when(userLookupService.getPersistenceReference(userId)).thenReturn(owner);

        shelfEventService.record(shelf, userId, ShelfEventType.SHELF_UPDATED, null);

        verify(shelfEventRepository).save(any(ShelfEvent.class));
    }

    @Test
    void listUsesDisplayNameWhenPresent() {
        User actor = new User(ACTOR_EXAMPLE_COM, HASH, ADA, LOVELACE);
        actor.setFirstName(ADA);
        actor.setLastName(LOVELACE);
        setId(actor, UUID.randomUUID());
        ShelfEvent event = new ShelfEvent(shelf, actor, ShelfEventType.SHELF_UPDATED, null);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent().getFirst().actorDisplayName())
                .isEqualTo(ADA_LOVELACE);
    }

    @Test
    void listFallsBackToReaderWhenNoNameAvailable() {
        User actor = new User(ACTOR_EXAMPLE_COM, HASH, "  ", null);
        actor.setFirstName("  ");
        actor.setLastName(null);
        setId(actor, UUID.randomUUID());
        ShelfEvent event = new ShelfEvent(shelf, actor, ShelfEventType.SHELF_UPDATED, null);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent().getFirst().actorDisplayName())
                .isEqualTo(TestConstants.READER_DISPLAY_NAME);
    }

    @Test
    void listUsesFirstNameWhenDisplayNameBlank() {
        User actor = new User(ACTOR_EXAMPLE_COM, HASH, "", null);
        actor.setFirstName(ADA);
        actor.setLastName(null);
        setId(actor, UUID.randomUUID());
        ShelfEvent event = new ShelfEvent(shelf, actor, ShelfEventType.SHELF_UPDATED, null);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent().getFirst().actorDisplayName())
                .isEqualTo(ADA);
    }

    @Test
    void listReturnsEmptyPayloadForInvalidJson() {
        ShelfEvent event = new ShelfEvent(shelf, owner, ShelfEventType.BOOK_ADDED, INVALID_JSON_PAYLOAD);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent().getFirst().payload())
                .isEmpty();
    }

    @Test
    void listDeserializesStoredPayload() {
        ShelfEvent event = new ShelfEvent(shelf, owner, ShelfEventType.BOOK_ADDED, BOOK_TITLE_PAYLOAD);
        setId(event, UUID.randomUUID());
        when(shelfEventRepository.findByShelf_IdOrderByCreatedAtDesc(shelfId, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(shelfEventService.list(shelfId, PageRequest.of(0, 10)).getContent().getFirst().payload())
                .containsEntry("bookTitle", "Dune");
    }

    @Test
    void listByTypesFiltersEventTypes() {
        when(shelfEventRepository.findByShelf_IdAndEventTypeInOrderByCreatedAtDesc(
                eq(shelfId), eq(ShelfEventService.BOOK_CHANGE_TYPES), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        assertThat(shelfEventService.listByTypes(shelfId, ShelfEventService.BOOK_CHANGE_TYPES, PageRequest.of(0, 10))
                .getContent()).isEmpty();
    }
}
