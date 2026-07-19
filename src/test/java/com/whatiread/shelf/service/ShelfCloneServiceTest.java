package com.whatiread.shelf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.identity.domain.User;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.domain.UserBook;
import com.whatiread.library.port.UserBookPersistencePort;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shelf.domain.Shelf;
import com.whatiread.shelf.domain.ShelfBook;
import com.whatiread.shelf.repository.ShelfBookRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShelfCloneServiceTest {

    private static final String HASH = "hash";
    private static final String USER = "User";
    private static final String DUNE = "Dune";
    @Mock
    private ShelfBookRepository shelfBookRepository;
    @Mock
    private UserBookPersistencePort userBookPersistencePort;
    @Mock
    private LibraryService libraryService;

    @InjectMocks
    private ShelfCloneService shelfCloneService;

    private UUID sourceShelfId;
    private UUID cloneShelfId;
    private UUID ownerId;
    private User owner;
    private Shelf source;
    private Shelf clone;

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
    }

    private static UserBookDto userBookDto(UUID userBookId, Book book) {
        BookDto bookDto = new BookDto(
                book.getId(), book.getTitle(), null, book.getAuthors(), null, book.getPageCount(), null,
                null, null, BookSource.MANUAL, null, null, 0, null, null,
                Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"));
        return new UserBookDto(
                userBookId, bookDto, ReadingStatus.TO_READ, null, null, null, null, null,
                null, null, List.of(), Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        sourceShelfId = UUID.randomUUID();
        cloneShelfId = UUID.randomUUID();
        owner = new User("owner@example.com", "owner", HASH, "Owner", USER);
        setId(owner, ownerId);
        source = new Shelf(owner, "Source", "source");
        clone = new Shelf(owner, "Clone", "clone");
        setId(source, sourceShelfId);
        setId(clone, cloneShelfId);
    }

    @Test
    void cloneWithBooksCopiesVisibleBooksOnly() {
        Book visibleBook = new Book();
        visibleBook.setTitle(DUNE);
        setId(visibleBook, UUID.randomUUID());
        Book hiddenBook = new Book();
        hiddenBook.setTitle("Hidden");
        setId(hiddenBook, UUID.randomUUID());
        UserBook visibleUserBook = new UserBook(owner, visibleBook, ReadingStatus.READ);
        setId(visibleUserBook, UUID.randomUUID());
        UserBook hiddenUserBook = new UserBook(owner, hiddenBook, ReadingStatus.READ);
        setId(hiddenUserBook, UUID.randomUUID());
        ShelfBook visible = new ShelfBook(source, visibleUserBook, 0, ownerId);
        ShelfBook hidden = new ShelfBook(source, hiddenUserBook, 1, ownerId);
        UserBook clonedUserBook = new UserBook(owner, visibleBook, ReadingStatus.READ);
        UUID clonedUserBookId = UUID.randomUUID();
        setId(clonedUserBook, clonedUserBookId);

        when(shelfBookRepository.findByShelf_IdOrderByPositionAsc(sourceShelfId)).thenReturn(List.of(visible, hidden));
        when(libraryService.ensureInLibrary(ownerId, visibleBook.getId())).thenReturn(userBookDto(clonedUserBookId, visibleBook));
        when(userBookPersistencePort.getOwnedReference(ownerId, clonedUserBookId)).thenReturn(clonedUserBook);

        shelfCloneService.cloneWithBooks(source, clone, ownerId, true, shelfBook -> shelfBook == visible);

        verify(shelfBookRepository).save(any(ShelfBook.class));
        assertThat(clone.getId()).isEqualTo(cloneShelfId);
    }

    @Test
    void cloneWithBooksSkipsWhenDisabled() {
        shelfCloneService.cloneWithBooks(source, clone, ownerId, false, shelfBook -> true);

        verify(shelfBookRepository, org.mockito.Mockito.never()).findByShelf_IdOrderByPositionAsc(any());
    }
}
