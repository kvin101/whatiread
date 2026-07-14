package com.whatiread.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.api.CreateBookRequest;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.catalog.integration.OpenLibraryClient;
import com.whatiread.catalog.integration.OpenLibraryDoc;
import com.whatiread.catalog.integration.OpenLibrarySearchResponse;
import com.whatiread.catalog.port.UserBookRatingProvider;
import com.whatiread.catalog.repository.BookRepository;
import com.whatiread.config.CacheConfig;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {


    private static final String GEORGE_ORWELL = "George Orwell";
    private static final String NEW_TITLE = "New Title";
    private static final String UNKNOWN = "Unknown";
    private static final String UNKNOWN_2 = "unknown";
    private static final String DUNE = "dune";
    private static final String V_4_0 = "4.0";
    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMetadataService bookMetadataService;

    @Mock
    private OpenLibraryClient openLibraryClient;

    @Mock
    private UserBookRatingProvider userBookRatingProvider;

    @Mock
    private CacheManager cacheManager;
    @Spy
    private BookMapper bookMapper = new BookMapper();

    @InjectMocks
    private BookServiceImpl bookService;

    private UUID bookId;
    private Book book;

    private static void setId(Book book, UUID id) throws Exception {
        Field idField = Book.class.getSuperclass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(book, id);
        Field createdAt = Book.class.getSuperclass().getSuperclass().getDeclaredField("createdAt");
        createdAt.setAccessible(true);
        createdAt.set(book, Instant.parse("2024-01-01T00:00:00Z"));
        Field updatedAt = Book.class.getSuperclass().getSuperclass().getDeclaredField("updatedAt");
        updatedAt.setAccessible(true);
        updatedAt.set(book, Instant.parse("2024-01-02T00:00:00Z"));
    }

    @BeforeEach
    void setUp() throws Exception {
        bookId = UUID.randomUUID();
        book = new Book();
        book.setTitle("1984");
        book.setAuthors(List.of(GEORGE_ORWELL));
        book.setIsbn("9780141036144");
        book.setPageCount(328);
        setId(book, bookId);
        lenient().when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(cacheManager.getCache(CacheConfig.BOOK_BY_ID)).thenReturn(null);
    }

    @Test
    void findOrCreateByIsbnReturnsExistingBook() {
        when(bookRepository.findByIsbn("9780141036144")).thenReturn(Optional.of(book));

        BookDto dto = bookService.findOrCreateByIsbn("9780141036144");

        assertThat(dto.id()).isEqualTo(bookId);
        assertThat(dto.title()).isEqualTo("1984");
    }

    @Test
    void findOrCreateByIsbnFetchesFromOpenLibraryWhenMissing() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                "1984",
                List.of(GEORGE_ORWELL),
                List.of("9780141036144"),
                1L,
                328,
                "/works/OL123W"
        );
        when(bookRepository.findByIsbn("9780141036144")).thenReturn(Optional.empty());
        when(openLibraryClient.search("isbn:9780141036144", 1, 1))
                .thenReturn(new OpenLibrarySearchResponse(List.of(doc), 1));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            setId(saved, bookId);
            return saved;
        });

        BookDto dto = bookService.findOrCreateByIsbn("9780141036144");

        assertThat(dto.id()).isEqualTo(bookId);
        verify(bookMetadataService).applyOpenLibraryDoc(any(Book.class), eq(doc));
    }

    @Test
    void findOrCreateByIsbnThrowsWhenOpenLibraryHasNoMatch() {
        when(bookRepository.findByIsbn("0000000000000")).thenReturn(Optional.empty());
        when(openLibraryClient.search("isbn:0000000000000", 1, 1))
                .thenReturn(new OpenLibrarySearchResponse(List.of(), 0));

        assertThatThrownBy(() -> bookService.findOrCreateByIsbn("0000000000000"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findOrCreateForImportReusesExistingBook() {
        when(bookRepository.findByIsbn("9780141036144")).thenReturn(Optional.of(book));

        BookDto dto = bookService.findOrCreateForImport(
                "1984", List.of(GEORGE_ORWELL), 328, "9780141036144");

        assertThat(dto.id()).isEqualTo(bookId);
    }

    @Test
    void searchLinksExternalResultsToCatalog() {
        BookSearchResultDto external = new BookSearchResultDto(
                null, "Dune", List.of("Frank Herbert"), "9780441172719", 688,
                null, BookSource.OPEN_LIBRARY, "ol-1");
        when(bookMetadataService.searchExternal(DUNE, 0, 10)).thenReturn(List.of(external));
        when(bookRepository.findByIsbn("9780441172719")).thenReturn(Optional.of(book));

        Page<?> page = bookService.search(DUNE, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(((BookSearchResultDto) page.getContent().getFirst()).id()).isEqualTo(bookId);
    }

    @Test
    void refreshAggregatedRatingUpdatesBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(userBookRatingProvider.findRatingsForBook(bookId)).thenReturn(List.of(
                new BigDecimal(V_4_0), new BigDecimal("5.0")));
        when(bookRepository.save(book)).thenReturn(book);

        bookService.refreshAggregatedRating(bookId);

        assertThat(book.getAverageRating()).isEqualByComparingTo("4.50");
        assertThat(book.getRatingCount()).isEqualTo(2);
    }

    @Test
    void createManualReusesExistingIsbnMatch() {
        CreateBookRequest request = new CreateBookRequest(
                "1984", null, List.of(GEORGE_ORWELL), "9780141036144",
                328, null, null, null, null);
        when(bookRepository.findByIsbn("9780141036144")).thenReturn(Optional.of(book));

        BookDto dto = bookService.createManual(request);

        assertThat(dto.id()).isEqualTo(bookId);
    }

    @Test
    void getByIdReturnsCachedBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        BookDto dto = bookService.getById(bookId);

        assertThat(dto.title()).isEqualTo("1984");
    }

    @Test
    void resolveCanonicalBookIdReturnsExistingId() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.findByIsbn("9780141036144")).thenReturn(Optional.of(book));

        assertThat(bookService.resolveCanonicalBookId(bookId)).isEqualTo(bookId);
    }

    @Test
    void refreshAggregatedRatingClearsRatingWhenNoRatings() {
        book.setAverageRating(new BigDecimal(V_4_0));
        book.setRatingCount(2);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(userBookRatingProvider.findRatingsForBook(bookId)).thenReturn(List.of());
        when(bookRepository.save(book)).thenReturn(book);

        bookService.refreshAggregatedRating(bookId);

        assertThat(book.getAverageRating()).isNull();
        assertThat(book.getRatingCount()).isZero();
    }

    @Test
    void createManualCreatesNewBookWhenMissing() {
        CreateBookRequest request = new CreateBookRequest(
                NEW_TITLE, null, List.of("Author"), null,
                200, null, "A story", "ext-1", BookSource.MANUAL);
        when(bookRepository.findByNormalizedTitle(NEW_TITLE)).thenReturn(List.of());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            setId(saved, bookId);
            return saved;
        });

        BookDto dto = bookService.createManual(request);

        assertThat(dto.title()).isEqualTo(NEW_TITLE);
    }

    @Test
    void getOrThrowReturnsBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThat(bookService.getOrThrow(bookId).title()).isEqualTo("1984");
    }

    @Test
    void searchReturnsUnlinkedExternalResults() {
        BookSearchResultDto external = new BookSearchResultDto(
                null, UNKNOWN, List.of("Author"), null, 100,
                null, BookSource.OPEN_LIBRARY, "ol-unknown");
        when(bookMetadataService.searchExternal(UNKNOWN_2, 0, 10)).thenReturn(List.of(external));
        when(bookRepository.findByNormalizedTitle(UNKNOWN)).thenReturn(List.of());

        Page<?> page = bookService.search(UNKNOWN_2, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(((BookSearchResultDto) page.getContent().getFirst()).id()).isNull();
    }

    @Test
    void createManualEnrichesExistingBookMetadata() {
        book.setIsbn(null);
        book.setPageCount(null);
        CreateBookRequest request = new CreateBookRequest(
                "1984", null, List.of(GEORGE_ORWELL), "9780141036144",
                328, "http://cover", "desc", null, BookSource.MANUAL);
        when(bookRepository.findByIsbn("9780141036144")).thenReturn(Optional.of(book));
        Cache cache = org.mockito.Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheConfig.BOOK_BY_ID)).thenReturn(cache);

        BookDto dto = bookService.createManual(request);

        assertThat(dto.isbn()).isEqualTo("9780141036144");
        assertThat(book.getPageCount()).isEqualTo(328);
    }
}
