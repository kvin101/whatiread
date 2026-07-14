package com.whatiread.shelf.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.identity.domain.User;
import com.whatiread.library.domain.UserBook;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfBookQueryPortImplTest {


    private static final String FAVORITES = "Favorites";
    @Mock
    private ShelfBookRepository shelfBookRepository;

    @InjectMocks
    private ShelfBookQueryPortImpl port;

    private UUID userId;
    private UUID shelfId;
    private UUID userBookId;

    private static User user(UUID id) {
        User user = new User("u@example.com", "ada_user", "hash", "Ada", "Lovelace");
        setId(user, id);
        return user;
    }

    private static Shelf shelf(User owner, String name) {
        Shelf shelf = new Shelf(owner, name, name.toLowerCase());
        setId(shelf, UUID.randomUUID());
        return shelf;
    }

    private static ShelfBook shelfBook(Shelf shelf) {
        UserBook userBook = new UserBook();
        setId(userBook, UUID.randomUUID());
        return new ShelfBook(shelf, userBook, 0, shelf.getOwner().getId());
    }

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id field on " + entity.getClass());
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        shelfId = UUID.randomUUID();
        userBookId = UUID.randomUUID();
    }

    @Test
    void findUserBookIdsOnShelfDelegatesToRepository() {
        List<UUID> ids = List.of(userBookId);
        when(shelfBookRepository.findUserBookIdsByShelfAndOwner(userId, shelfId)).thenReturn(ids);

        assertThat(port.findUserBookIdsOnShelf(userId, shelfId)).isEqualTo(ids);
    }

    @Test
    void getShelfNamesByUserBookIdsFiltersByOwner() throws Exception {
        User owner = user(userId);
        User otherOwner = user(UUID.randomUUID());
        Shelf ownedShelf = shelf(owner, FAVORITES);
        Shelf foreignShelf = shelf(otherOwner, "Other");
        ShelfBook owned = shelfBook(ownedShelf);
        ShelfBook foreign = shelfBook(foreignShelf);

        when(shelfBookRepository.findByUserBook_Id(userBookId)).thenReturn(List.of(owned, foreign));

        Map<UUID, List<String>> names = port.getShelfNamesByUserBookIds(userId, List.of(userBookId));

        assertThat(names).containsEntry(userBookId, List.of(FAVORITES));
    }

    @Test
    void existsOnShelfDelegatesToRepository() {
        when(shelfBookRepository.existsByShelf_IdAndUserBook_Id(shelfId, userBookId)).thenReturn(true);

        assertThat(port.existsOnShelf(shelfId, userBookId)).isTrue();
    }
}
