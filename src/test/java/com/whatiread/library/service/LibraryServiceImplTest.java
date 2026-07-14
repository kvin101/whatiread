package com.whatiread.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.catalog.service.BookService;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.library.api.AddToLibraryRequest;
import com.whatiread.library.api.CreateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookRequest;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.domain.UserBook;
import com.whatiread.library.domain.UserBookNote;
import com.whatiread.library.port.ShelfBookQueryPort;
import com.whatiread.library.repository.UserBookNoteRepository;
import com.whatiread.library.repository.UserBookRepository;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibraryServiceImplTest {


    private static final String DUNE = "dune";
    private static final String DUNE_2 = "Dune";
    private static final String INSIGHT = "Insight";
    private static final String FRANK_HERBERT = "Frank Herbert";
    private static final String V_2024_06_01 = "2024-06-01";
    private static final String UPDATED = "Updated";
    private static final String V_4_5 = "4.5";
    @Mock
    private UserBookRepository userBookRepository;
    @Mock
    private UserBookNoteRepository userBookNoteRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private BookService bookService;
    @Mock
    private BookPersistencePort bookPersistencePort;
    @Mock
    private ShelfBookQueryPort shelfBookQueryPort;
    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private LibraryServiceImpl libraryService;

    private UUID userId;
    private UUID bookId;
    private UUID userBookId;
    private User user;
    private Book book;
    private UserBook userBook;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                setTimestamp(entity, type, "createdAt");
                setTimestamp(entity, type, "updatedAt");
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id on " + entity.getClass());
    }

    private static void setTimestamp(Object entity, Class<?> start, String fieldName) {
        Class<?> type = start;
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(entity, Instant.parse("2024-01-01T00:00:00Z"));
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        userBookId = UUID.randomUUID();
        user = new User("reader@example.com", "reader", "hash", "Reader", "User");
        setId(user, userId);
        book = new Book();
        book.setTitle(DUNE_2);
        book.setAuthors(List.of(FRANK_HERBERT));
        book.setPageCount(688);
        setId(book, bookId);
        userBook = new UserBook(user, book, ReadingStatus.READING);
        setId(userBook, userBookId);
        when(bookService.getById(bookId)).thenReturn(bookDto());
        when(userBookRepository.save(any(UserBook.class))).thenAnswer(invocation -> {
            UserBook saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                setId(saved, userBookId);
            }
            return saved;
        });
    }

    @Test
    void addRejectsDuplicateWork() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.of(userBook));

        assertThatThrownBy(() -> libraryService.add(userId, new AddToLibraryRequest(bookId, ReadingStatus.TO_READ, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addCreatesLibraryEntry() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.empty());
        when(userLookupService.getPersistenceReference(userId)).thenReturn(user);
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);

        var dto = libraryService.add(userId, new AddToLibraryRequest(bookId, ReadingStatus.TO_READ, 10));

        assertThat(dto.status()).isEqualTo(ReadingStatus.TO_READ);
        verify(businessMetrics).recordBookAddedToLibrary();
    }

    @Test
    void listReturnsEmptyPageWhenShelfHasNoBooks() {
        UUID shelfId = UUID.randomUUID();
        when(shelfBookQueryPort.findUserBookIdsOnShelf(userId, shelfId)).thenReturn(List.of());

        Page<?> page = libraryService.list(userId, null, shelfId, null, PageRequest.of(0, 10));

        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    void updateRejectsInvalidRatingIncrement() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        assertThatThrownBy(() -> libraryService.update(
                userId, userBookId, new UpdateUserBookRequest(null, new BigDecimal("3.3"), null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0.5 increments");
    }

    @Test
    void updateRefreshesAggregatedRatingWhenRated() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        libraryService.update(
                userId, userBookId, new UpdateUserBookRequest(null, new BigDecimal(V_4_5), null, null, null, null, null));

        verify(bookService).refreshAggregatedRating(bookId);
    }

    @Test
    void getSharedViewStripsNotes() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        var dto = libraryService.getSharedView(userId, userBookId);

        assertThat(dto.notes()).isEmpty();
        assertThat(dto.book().title()).isEqualTo(DUNE_2);
    }

    @Test
    void getOwnerIdReturnsLibraryOwner() {
        when(userBookRepository.findById(userBookId)).thenReturn(Optional.of(userBook));

        assertThat(libraryService.getOwnerId(userBookId)).isEqualTo(userId);
    }

    @Test
    void upsertForImportUpdatesExistingStatus() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.of(userBook));

        var dto = libraryService.upsertForImport(userId, bookId, ReadingStatus.READ);

        assertThat(dto.status()).isEqualTo(ReadingStatus.READ);
    }

    @Test
    void updateFinishedDateIfReadSetsDateOnlyForReadBooks() {
        userBook.setStatus(ReadingStatus.READ);
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));
        LocalDate finished = LocalDate.parse(V_2024_06_01);

        var dto = libraryService.updateFinishedDateIfRead(userId, userBookId, finished);

        assertThat(dto.finishedAt()).isEqualTo(finished);
    }

    @Test
    void listDedupesDuplicateWorks() {
        UserBook duplicate = new UserBook(user, book, ReadingStatus.TO_READ);
        setId(duplicate, UUID.randomUUID());
        when(userBookRepository.findByUserId(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(userBook, duplicate)));

        Page<?> page = libraryService.list(userId, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listWithCursorReturnsKeysetPage() {
        when(userBookRepository.findKeysetByUser(eq(userId), eq(ReadingStatus.READ), any(), any(), any()))
                .thenReturn(List.of(userBook));

        CursorPage<?> page = libraryService.listWithCursor(userId, ReadingStatus.READ, null, null, null, 10);

        assertThat(page.items()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void listWithCursorUsesOffsetListWhenSearching() {
        when(userBookRepository.searchByUserId(userId, DUNE, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(userBook)));

        CursorPage<?> page = libraryService.listWithCursor(userId, null, null, DUNE, null, 5);

        assertThat(page.items()).hasSize(1);
    }

    @Test
    void listNotesReturnsOrderedNotes() {
        UserBookNote note = new UserBookNote(userBook, user, "Great read");
        setId(note, UUID.randomUUID());
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));
        when(userBookNoteRepository.findByUserBookIdOrderByCreatedAtAsc(userBookId)).thenReturn(List.of(note));

        assertThat(libraryService.listNotes(userId, userBookId)).hasSize(1);
    }

    @Test
    void addNotePersistsTrimmedBody() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));
        when(userLookupService.getPersistenceReference(userId)).thenReturn(user);
        UserBookNote saved = new UserBookNote(userBook, user, INSIGHT);
        setId(saved, UUID.randomUUID());
        when(userBookNoteRepository.save(any(UserBookNote.class))).thenReturn(saved);

        var dto = libraryService.addNote(userId, userBookId, new CreateUserBookNoteRequest("  Insight  "));

        assertThat(dto.body()).isEqualTo(INSIGHT);
    }

    @Test
    void updateNoteChangesBody() {
        UserBookNote note = new UserBookNote(userBook, user, "Old");
        UUID noteId = UUID.randomUUID();
        setId(note, noteId);
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));
        when(userBookNoteRepository.findByIdAndUserBook_IdAndUserBook_User_Id(noteId, userBookId, userId))
                .thenReturn(Optional.of(note));
        when(userBookNoteRepository.save(note)).thenReturn(note);

        var dto = libraryService.updateNote(
                userId, userBookId, noteId, new UpdateUserBookNoteRequest(UPDATED));

        assertThat(dto.body()).isEqualTo(UPDATED);
    }

    @Test
    void deleteNoteRemovesEntry() {
        UserBookNote note = new UserBookNote(userBook, user, "Remove me");
        UUID noteId = UUID.randomUUID();
        setId(note, noteId);
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));
        when(userBookNoteRepository.findByIdAndUserBook_IdAndUserBook_User_Id(noteId, userBookId, userId))
                .thenReturn(Optional.of(note));

        libraryService.deleteNote(userId, userBookId, noteId);

        verify(userBookNoteRepository).delete(note);
        verify(userBookNoteRepository).flush();
    }

    @Test
    void deleteRefreshesRatingWhenBookWasRated() {
        userBook.setRating(new BigDecimal(V_4_5));
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        libraryService.delete(userId, userBookId);

        verify(userBookRepository).delete(userBook);
        verify(bookService).refreshAggregatedRating(bookId);
    }

    @Test
    void listSearchByStatusAndShelf() {
        UUID shelfId = UUID.randomUUID();
        when(shelfBookQueryPort.findUserBookIdsOnShelf(userId, shelfId)).thenReturn(List.of(userBookId));
        when(userBookRepository.searchByUserIdAndStatusAndIdIn(
                eq(userId), eq(ReadingStatus.READING), eq(List.of(userBookId)), eq(DUNE), any()))
                .thenReturn(new PageImpl<>(List.of(userBook)));

        Page<?> page = libraryService.list(userId, ReadingStatus.READING, shelfId, DUNE, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getByBookIdReturnsLibraryEntry() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.of(userBook));

        assertThat(libraryService.getByBookId(userId, bookId).id()).isEqualTo(userBookId);
    }

    @Test
    void ensureInLibraryReturnsExistingEntry() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.of(userBook));

        assertThat(libraryService.ensureInLibrary(userId, bookId).id()).isEqualTo(userBookId);
    }

    @Test
    void ensureInLibraryAddsMissingBook() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.empty());
        when(userLookupService.getPersistenceReference(userId)).thenReturn(user);
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);

        assertThat(libraryService.ensureInLibrary(userId, bookId).id()).isEqualTo(userBookId);
    }

    @Test
    void listWithCursorEncodesNextCursorWhenMoreResultsExist() {
        UserBook second = new UserBook(user, book, ReadingStatus.READ);
        UUID secondId = UUID.randomUUID();
        setId(second, secondId);
        when(userBookRepository.findKeysetByUser(eq(userId), eq(ReadingStatus.READ), any(), any(), any()))
                .thenReturn(List.of(userBook, second));

        CursorPage<?> page = libraryService.listWithCursor(userId, ReadingStatus.READ, null, null, null, 1);

        assertThat(page.items()).hasSize(1);
        assertThat(page.nextCursor()).isNotNull();
        assertThat(page.hasMore()).isTrue();
    }

    @Test
    void deleteSkipsRatingRefreshWhenUnrated() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        libraryService.delete(userId, userBookId);

        verify(userBookRepository).delete(userBook);
        verify(bookService, never()).refreshAggregatedRating(any());
    }

    @Test
    void updateClearsFinishedDateWhenLeavingReadStatus() {
        userBook.setStatus(ReadingStatus.READ);
        userBook.setFinishedAt(LocalDate.parse("2024-01-01"));
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        libraryService.update(
                userId, userBookId, new UpdateUserBookRequest(ReadingStatus.READING, null, null, null, null, null, null));

        assertThat(userBook.getFinishedAt()).isNull();
    }

    @Test
    void listByStatusUsesStatusFilter() {
        when(userBookRepository.findByUserIdAndStatus(userId, ReadingStatus.READING, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(userBook)));

        Page<?> page = libraryService.list(userId, ReadingStatus.READING, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findUserBookForWorkMatchesByNormalizedTitle() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.empty());
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);
        when(userBookRepository.findByUserIdAndNormalizedBookTitle(userId, DUNE_2)).thenReturn(List.of(userBook));

        assertThat(libraryService.hasBook(userId, bookId)).isTrue();
    }

    @Test
    void listWithCursorDecodesExistingCursor() {
        String cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                (userBook.getUpdatedAt().toEpochMilli() + ":" + userBookId).getBytes());
        when(userBookRepository.findKeysetByUser(
                eq(userId), eq(null), eq(userBook.getUpdatedAt()), eq(userBookId), any()))
                .thenReturn(List.of(userBook));

        CursorPage<?> page = libraryService.listWithCursor(userId, null, null, null, cursor, 10);

        assertThat(page.items()).hasSize(1);
    }

    @Test
    void getReturnsNotesOnOwnedBook() {
        UserBookNote note = new UserBookNote(userBook, user, INSIGHT);
        setId(note, UUID.randomUUID());
        userBook.addNote(note);
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        assertThat(libraryService.get(userId, userBookId).notes()).hasSize(1);
    }

    @Test
    void listAllForUserReturnsLibraryEntries() {
        when(userBookRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(userBook));

        assertThat(libraryService.listAllForUser(userId)).hasSize(1);
    }

    @Test
    void listWithShelfAndSearchUsesShelfScopedSearch() {
        UUID shelfId = UUID.randomUUID();
        when(shelfBookQueryPort.findUserBookIdsOnShelf(userId, shelfId)).thenReturn(List.of(userBookId));
        when(userBookRepository.searchByUserIdAndIdIn(userId, List.of(userBookId), DUNE, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(userBook)));

        Page<?> page = libraryService.list(userId, null, shelfId, DUNE, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listWithShelfAndStatusUsesCombinedFilter() {
        UUID shelfId = UUID.randomUUID();
        when(shelfBookQueryPort.findUserBookIdsOnShelf(userId, shelfId)).thenReturn(List.of(userBookId));
        when(userBookRepository.findByUserIdAndStatusAndIdIn(
                userId, ReadingStatus.READING, List.of(userBookId), PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(userBook)));

        Page<?> page = libraryService.list(userId, ReadingStatus.READING, shelfId, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void updateRejectsRatingAboveMaximum() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        assertThatThrownBy(() -> libraryService.update(
                userId, userBookId, new UpdateUserBookRequest(null, new BigDecimal("5.5"), null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0.5 and 5.0");
    }

    @Test
    void updateClearsRatingWhenRequested() {
        userBook.setRating(new BigDecimal("4.0"));
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        libraryService.update(
                userId, userBookId, new UpdateUserBookRequest(null, null, true, null, null, null, null));

        assertThat(userBook.getRating()).isNull();
        verify(bookService).refreshAggregatedRating(bookId);
    }

    @Test
    void updateFinishedDateIfReadIgnoresNonReadStatus() {
        when(userBookRepository.findByIdAndUserId(userBookId, userId)).thenReturn(Optional.of(userBook));

        var dto = libraryService.updateFinishedDateIfRead(
                userId, userBookId, LocalDate.parse(V_2024_06_01));

        assertThat(dto.finishedAt()).isNull();
        verify(userBookRepository, never()).save(any());
    }

    @Test
    void listWithCursorDelegatesToOffsetListWhenShelfProvided() {
        UUID shelfId = UUID.randomUUID();
        when(shelfBookQueryPort.findUserBookIdsOnShelf(userId, shelfId)).thenReturn(List.of(userBookId));
        when(userBookRepository.findByUserIdAndIdIn(userId, List.of(userBookId), PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(userBook)));

        CursorPage<?> page = libraryService.listWithCursor(userId, null, shelfId, null, null, 5);

        assertThat(page.items()).hasSize(1);
    }

    @Test
    void getByBookIdThrowsWhenMissing() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.empty());
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);
        when(userBookRepository.findByUserIdAndNormalizedBookTitle(userId, DUNE_2)).thenReturn(List.of());

        assertThatThrownBy(() -> libraryService.getByBookId(userId, bookId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addSetsStartedAtWhenStatusIsReading() {
        when(bookService.resolveCanonicalBookId(bookId)).thenReturn(bookId);
        when(userBookRepository.findByUserIdAndBook_Id(userId, bookId)).thenReturn(Optional.empty());
        when(userLookupService.getPersistenceReference(userId)).thenReturn(user);
        when(bookPersistencePort.getReference(bookId)).thenReturn(book);
        when(userBookRepository.save(any(UserBook.class))).thenAnswer(invocation -> {
            UserBook saved = invocation.getArgument(0);
            assertThat(saved.getStartedAt()).isNotNull();
            return saved;
        });

        libraryService.add(userId, new AddToLibraryRequest(bookId, ReadingStatus.READING, null));
    }

    private BookDto bookDto() {
        return new BookDto(
                bookId, DUNE_2, null, List.of(FRANK_HERBERT), null, 688,
                null, null, BookSource.OPEN_LIBRARY, null, null, 0, null, null,
                Instant.now(), Instant.now());
    }
}
