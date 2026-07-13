package com.whatiread.shelf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.identity.domain.User;
import com.whatiread.shelf.domain.ExploreShelfReadModel;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.repository.ExploreShelfReadModelRepository;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExploreShelfReadModelServiceTest {


    private static final String READING = "Reading";
    @Mock
    private ExploreShelfReadModelRepository repository;
    @Mock
    private ShelfBookRepository shelfBookRepository;

    @InjectMocks
    private ExploreShelfReadModelService exploreShelfReadModelService;

    private UUID shelfId;
    private Shelf shelf;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                Field updatedAt = findField(type, "updatedAt");
                if (updatedAt != null) {
                    updatedAt.setAccessible(true);
                    updatedAt.set(entity, Instant.parse("2024-01-02T00:00:00Z"));
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
        shelfId = UUID.randomUUID();
        User owner = new User("owner@example.com", "hash", "Owner", "User");
        setId(owner, UUID.randomUUID());
        shelf = new Shelf(owner, READING, "reading");
        setId(shelf, shelfId);
        shelf.setVisibility(ShelfVisibility.PUBLIC);
    }

    @Test
    void syncUpsertsReadModel() {
        when(repository.findById(shelfId)).thenReturn(Optional.empty());
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(3L);

        exploreShelfReadModelService.sync(shelf);

        verify(repository).save(any(ExploreShelfReadModel.class));
    }

    @Test
    void removeDeletesReadModel() {
        exploreShelfReadModelService.remove(shelfId);

        verify(repository).deleteById(shelfId);
    }

    @Test
    void syncUpdatesExistingReadModel() {
        ExploreShelfReadModel existing = new ExploreShelfReadModel();
        when(repository.findById(shelfId)).thenReturn(Optional.of(existing));
        when(shelfBookRepository.countByShelf_Id(shelfId)).thenReturn(1L);

        exploreShelfReadModelService.sync(shelf);

        assertThat(existing.getName()).isEqualTo(READING);
        verify(repository).save(existing);
    }

    @Test
    void encodeAndDecodeCursorRoundTrip() {
        Instant updatedAt = Instant.parse("2024-06-01T00:00:00Z");
        String encoded = ExploreShelfReadModelService.encodeCursor(updatedAt, shelfId);
        ExploreShelfReadModelService.CursorParts parts = ExploreShelfReadModelService.decodeCursor(encoded);

        assertThat(parts.shelfId()).isEqualTo(shelfId);
        assertThat(parts.updatedAt()).isEqualTo(updatedAt);
    }
}
