package com.whatiread.importexport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.importexport.api.GoodreadsImportResultDto;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.domain.ShelfVisibility;
import com.whatiread.shelf.service.ShelfService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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

@ExtendWith(MockitoExtension.class)
class GoodreadsCsvProcessorTest {


    private static final String DUNE = "Dune";
    private static final String FRANK_HERBERT = "Frank Herbert";
    private static final String FAVORITES = "Favorites";
    private static final String NEUROMANCER = "Neuromancer";
    @Mock
    private UserLookupService userLookupService;

    @Mock
    private ImportBookResolver importBookResolver;

    @Mock
    private LibraryService libraryService;

    @Mock
    private ShelfService shelfService;

    @Mock
    private ImportJobStatusService importJobStatusService;

    @Mock
    private BusinessMetrics businessMetrics;

    @InjectMocks
    private GoodreadsCsvProcessor processor;

    private UUID userId;
    private UUID bookId;
    private UUID shelfId;
    private UUID userBookId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        shelfId = UUID.randomUUID();
        userBookId = UUID.randomUUID();
    }

    @Test
    void importCsvProcessesValidRowAndCreatesShelf() {
        String csv = """
                Title,Author,ISBN13,My Rating,Exclusive Shelf,Bookshelves,Date Read,Number of Pages
                1984,George Orwell,9780141036144,5,read,Favorites,2020/01/15,328
                """;
        BookDto book = bookDto("1984");
        UserBookDto userBook = new UserBookDto(
                userBookId, book, ReadingStatus.READ, null, null, null, null, null,
                null, null, List.of(), Instant.now(), Instant.now());

        when(importBookResolver.resolveByIsbn("9780141036144")).thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.READ)).thenReturn(userBook);
        when(shelfService.findShelfIdByOwnerAndName(userId, FAVORITES)).thenReturn(Optional.empty());
        when(shelfService.create(eq(userId), any())).thenReturn(new ShelfDto(
                shelfId, FAVORITES, "favorites", null, null, ShelfVisibility.PRIVATE,
                0, userId, null, 0, Instant.now(), Instant.now(), "Reader"));
        when(shelfService.hasUserBookOnShelf(shelfId, userBookId)).thenReturn(false);

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.rowsProcessed()).isEqualTo(1);
        assertThat(result.booksImported()).isEqualTo(1);
        assertThat(result.shelvesCreated()).isEqualTo(1);
        verify(userLookupService).requireExists(userId);
        verify(shelfService).addBook(eq(userId), eq(shelfId), any());
    }

    @Test
    void importCsvSkipsRowsWithoutTitle() {
        String csv = """
                Title,Author
                ,George Orwell
                Dune,Frank Herbert
                """;
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = new UserBookDto(
                userBookId, book, ReadingStatus.TO_READ, null, null, null, null, null,
                null, null, List.of(), Instant.now(), Instant.now());

        when(importBookResolver.resolveForImport(DUNE, List.of(FRANK_HERBERT), null, null))
                .thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.TO_READ)).thenReturn(userBook);

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.booksImported()).isEqualTo(1);
    }

    @Test
    void importCsvRejectsEmptyFile() {
        assertThatThrownBy(() -> processor.importCsv(
                userId, new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void processJobMarksCompletedOnSuccess() {
        byte[] csv = """
                Title,Author
                Dune,Frank Herbert
                """.getBytes(StandardCharsets.UTF_8);
        UUID jobId = UUID.randomUUID();
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = new UserBookDto(
                userBookId, book, ReadingStatus.TO_READ, null, null, null, null, null,
                null, null, List.of(), Instant.now(), Instant.now());

        when(importBookResolver.resolveForImport(any(), any(), any(), any())).thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.TO_READ)).thenReturn(userBook);

        processor.processJob(jobId, userId, csv);

        verify(importJobStatusService).markProcessing(jobId);
        verify(importJobStatusService).markCompleted(eq(jobId), any());
        verify(importJobStatusService, never()).markFailed(any(), any());
    }

    @Test
    void importCsvSkipsDuplicateRows() {
        String csv = """
                Title,Author,ISBN13
                Dune,Frank Herbert,9780441172719
                Dune,Frank Herbert,9780441172719
                """;
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = userBookDto(book);

        when(importBookResolver.resolveByIsbn("9780441172719")).thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.TO_READ)).thenReturn(userBook);

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.duplicatesSkipped()).isEqualTo(1);
        assertThat(result.booksImported()).isEqualTo(1);
    }

    @Test
    void processJobMarksFailedOnError() {
        UUID jobId = UUID.randomUUID();
        processor.processJob(jobId, userId, "not valid csv".getBytes(StandardCharsets.UTF_8));

        verify(importJobStatusService).markProcessing(jobId);
        verify(importJobStatusService).markFailed(eq(jobId), any());
        verify(importJobStatusService, never()).markCompleted(any(), any());
    }

    @Test
    void importCsvFallsBackWhenIsbnLookupFails() {
        String csv = """
                Title,Author,ISBN13,Number of Pages
                Dune,Frank Herbert,978-0-441-17271-9,688
                """;
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = userBookDto(book);

        when(importBookResolver.resolveByIsbn("9780441172719"))
                .thenThrow(new ResourceNotFoundException("ISBN not found"));
        when(importBookResolver.resolveForImport(DUNE, List.of(FRANK_HERBERT), 688, "9780441172719"))
                .thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.TO_READ)).thenReturn(userBook);

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.booksImported()).isEqualTo(1);
    }

    @Test
    void importCsvCountsRowErrorsWithoutAborting() {
        String csv = """
                Title,Author
                Dune,Frank Herbert
                Broken,
                """;
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = userBookDto(book);

        when(importBookResolver.resolveForImport(DUNE, List.of(FRANK_HERBERT), null, null)).thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.TO_READ)).thenReturn(userBook);
        when(importBookResolver.resolveForImport("", List.of("Unknown"), null, null))
                .thenThrow(new IllegalStateException("bad row"));

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.booksImported()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
    }

    @Test
    void importCsvUsesExistingShelfAndSkipsSystemShelves() {
        String csv = """
                Title,Author,Bookshelves,Exclusive Shelf
                Dune,Frank Herbert,"read, Favorites",currently-reading
                """;
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = new UserBookDto(
                userBookId, book, ReadingStatus.READING, null, null, null, null, null,
                null, null, List.of(), Instant.now(), Instant.now());

        when(importBookResolver.resolveForImport(DUNE, List.of(FRANK_HERBERT), null, null)).thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.READING)).thenReturn(userBook);
        when(shelfService.findShelfIdByOwnerAndName(userId, FAVORITES)).thenReturn(Optional.of(shelfId));
        when(shelfService.hasUserBookOnShelf(shelfId, userBookId)).thenReturn(true);

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.shelvesCreated()).isZero();
        verify(shelfService, never()).addBook(any(), any(), any());
    }

    @Test
    void importCsvAppliesRatingAndFinishedDate() {
        String csv = """
                Title,Author,My Rating,Date Read,Exclusive Shelf
                Dune,Frank Herbert,4,2020-01-15,read
                """;
        BookDto book = bookDto(DUNE);
        UserBookDto userBook = new UserBookDto(
                userBookId, book, ReadingStatus.READ, null, null, null, null, null,
                null, null, List.of(), Instant.now(), Instant.now());

        when(importBookResolver.resolveForImport(DUNE, List.of(FRANK_HERBERT), null, null)).thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.READ)).thenReturn(userBook);

        processor.importCsv(userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        verify(libraryService).update(eq(userId), eq(userBookId), any());
        verify(libraryService).updateFinishedDateIfRead(userId, userBookId, LocalDate.parse("2020-01-15"));
    }

    @Test
    void importCsvResolvesBooksWithoutIsbn() {
        String csv = """
                Title,Author,Number of Pages
                Neuromancer,William Gibson,271
                """;
        BookDto book = bookDto(NEUROMANCER);
        UserBookDto userBook = userBookDto(book);

        when(importBookResolver.resolveForImport(NEUROMANCER, List.of("William Gibson"), 271, null))
                .thenReturn(book);
        when(libraryService.upsertForImport(userId, bookId, ReadingStatus.TO_READ)).thenReturn(userBook);

        GoodreadsImportResultDto result = processor.importCsv(
                userId, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.booksImported()).isEqualTo(1);
    }

    private BookDto bookDto(String title) {
        return new BookDto(
                bookId, title, null, List.of("Author"), "9780441172719", 300,
                null, null, BookSource.OPEN_LIBRARY, null, null, 0, null, null,
                Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-02T00:00:00Z"));
    }

    private UserBookDto userBookDto(BookDto book) {
        return new UserBookDto(
                userBookId, book, ReadingStatus.TO_READ, null, null, null, null, null,
                null, null, List.of(), Instant.now(), Instant.now());
    }
}
