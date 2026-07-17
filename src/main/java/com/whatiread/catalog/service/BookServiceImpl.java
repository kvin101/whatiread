package com.whatiread.catalog.service;

import com.whatiread.catalog.author.domain.Author;
import com.whatiread.catalog.author.service.AuthorService;
import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.api.CreateBookRequest;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookWorkMatcher;
import com.whatiread.catalog.integration.OpenLibraryClient;
import com.whatiread.catalog.port.UserBookRatingProvider;
import com.whatiread.catalog.repository.BookRepository;
import com.whatiread.config.CacheConfig;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMetadataService bookMetadataService;
    private final OpenLibraryClient openLibraryClient;
    private final UserBookRatingProvider userBookRatingProvider;
    private final CacheManager cacheManager;
    private final BookMapper bookMapper;
    private final AuthorService authorService;

    public BookServiceImpl(
            BookRepository bookRepository,
            BookMetadataService bookMetadataService,
            OpenLibraryClient openLibraryClient,
            UserBookRatingProvider userBookRatingProvider,
            CacheManager cacheManager,
            BookMapper bookMapper,
            @Lazy AuthorService authorService
    ) {
        this.bookRepository = bookRepository;
        this.bookMetadataService = bookMetadataService;
        this.openLibraryClient = openLibraryClient;
        this.userBookRatingProvider = userBookRatingProvider;
        this.cacheManager = cacheManager;
        this.bookMapper = bookMapper;
        this.authorService = authorService;
    }

    private static CreateBookRequest toCreateRequest(Book book) {
        return new CreateBookRequest(
                book.getTitle(),
                book.getSubtitle(),
                book.getAuthors(),
                book.getIsbn(),
                book.getPageCount(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getExternalId(),
                book.getSource()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookSearchResultDto> search(String query, Pageable pageable) {
        List<BookSearchResultDto> external = bookMetadataService.searchExternal(
                query,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        List<BookSearchResultDto> linked = external.stream()
                .map(this::linkToCatalogIfExists)
                .toList();
        return new PageImpl<>(linked, pageable, linked.size());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.BOOK_BY_ID, key = "#bookId")
    public BookDto getById(UUID bookId) {
        return bookMapper.toDto(getEntity(bookId));
    }

    @Override
    public BookDto createManual(CreateBookRequest request) {
        return findOrCreateFromRequest(request);
    }

    @Override
    public UUID resolveCanonicalBookId(UUID bookId) {
        Book book = getEntity(bookId);
        return findOrCreateFromRequest(toCreateRequest(book)).id();
    }

    @Override
    public BookDto findOrCreateByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .map(bookMapper::toDto)
                .orElseGet(() -> {
                    var response = openLibraryClient.search("isbn:" + isbn, 1, 1);
                    if (response.docs() == null || response.docs().isEmpty()) {
                        throw new ResourceNotFoundException("Book not found for ISBN " + isbn);
                    }
                    Book book = new Book();
                    bookMetadataService.applyOpenLibraryDoc(book, response.docs().getFirst());
                    book.setIsbn(isbn);
                    Book saved = bookRepository.save(book);
                    syncBookAuthors(saved);
                    return bookMapper.toDto(saved);
                });
    }

    @Override
    public BookDto findOrCreateForImport(String title, List<String> authors, Integer pageCount, String isbn) {
        return findOrCreateFromRequest(new CreateBookRequest(
                title,
                null,
                authors,
                isbn,
                pageCount,
                null,
                null,
                null,
                null
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public BookDto getOrThrow(UUID bookId) {
        return bookMapper.toDto(getEntity(bookId));
    }

    Book getEntity(UUID bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.BOOK_BY_ID, key = "#bookId")
    public void refreshAggregatedRating(UUID bookId) {
        Book book = getEntity(bookId);
        BookRatingAggregator.AggregatedRating aggregated = BookRatingAggregator.aggregate(
                userBookRatingProvider.findRatingsForBook(bookId)
        );
        book.setAverageRating(aggregated.averageRating());
        book.setRatingCount(aggregated.ratingCount());
        bookRepository.save(book);
    }

    private BookDto findOrCreateFromRequest(CreateBookRequest request) {
        Optional<Book> existing = findExistingBook(request);
        Book book = existing.isPresent()
                ? enrichIfNeeded(existing.get(), request)
                : createNewBook(request);
        syncBookAuthors(book);
        return bookMapper.toDto(book);
    }

    private Optional<Book> findExistingBook(CreateBookRequest request) {
        if (StringUtils.hasText(request.isbn())) {
            Optional<Book> byIsbn = bookRepository.findByIsbn(request.isbn());
            if (byIsbn.isPresent()) {
                return byIsbn;
            }
        }
        if (request.source() != null && StringUtils.hasText(request.externalId())) {
            Optional<Book> byExternal = bookRepository.findBySourceAndExternalId(
                    request.source(),
                    request.externalId()
            );
            if (byExternal.isPresent()) {
                return byExternal;
            }
        }
        List<String> normalizedAuthors = BookWorkMatcher.normalizeAuthors(request.authors());
        return bookRepository.findByNormalizedTitle(request.title().trim()).stream()
                .filter(book -> BookWorkMatcher.authorsMatch(book.getAuthors(), normalizedAuthors))
                .findFirst();
    }

    private Book createNewBook(CreateBookRequest request) {
        Book book = new Book();
        book.setTitle(request.title().trim());
        book.setSubtitle(request.subtitle());
        book.setAuthors(request.authors());
        book.setIsbn(request.isbn());
        book.setPageCount(request.pageCount());
        book.setCoverUrl(request.coverUrl());
        book.setDescription(request.description());
        book.setSource(request.source());
        book.setExternalId(request.externalId());
        return bookRepository.save(book);
    }

    private Book enrichIfNeeded(Book book, CreateBookRequest request) {
        boolean changed = false;
        if (!StringUtils.hasText(book.getIsbn()) && StringUtils.hasText(request.isbn())) {
            book.setIsbn(request.isbn());
            changed = true;
        }
        if (!StringUtils.hasText(book.getExternalId()) && StringUtils.hasText(request.externalId())) {
            book.setExternalId(request.externalId());
            changed = true;
        }
        if (book.getSource() == null && request.source() != null) {
            book.setSource(request.source());
            changed = true;
        }
        if (!StringUtils.hasText(book.getCoverUrl()) && StringUtils.hasText(request.coverUrl())) {
            book.setCoverUrl(request.coverUrl());
            changed = true;
        }
        if (book.getPageCount() == null && request.pageCount() != null) {
            book.setPageCount(request.pageCount());
            changed = true;
        }
        if (!StringUtils.hasText(book.getDescription()) && StringUtils.hasText(request.description())) {
            book.setDescription(request.description());
            changed = true;
        }
        return changed ? saveAndEvictCache(book) : book;
    }

    private Book saveAndEvictCache(Book book) {
        Book saved = bookRepository.save(book);
        evictBookCache(saved.getId());
        return saved;
    }

    private void evictBookCache(UUID bookId) {
        var cache = cacheManager.getCache(CacheConfig.BOOK_BY_ID);
        if (cache != null) {
            cache.evict(bookId);
        }
    }

    private void syncBookAuthors(Book book) {
        List<String> names = book.getAuthors();
        if (names == null || names.isEmpty()) {
            return;
        }
        for (int i = 0; i < names.size(); i++) {
            Author author = authorService.findOrCreateByName(names.get(i));
            authorService.linkBookAuthor(book, author, i);
        }
    }

    private BookSearchResultDto linkToCatalogIfExists(BookSearchResultDto result) {
        CreateBookRequest request = new CreateBookRequest(
                result.title(),
                null,
                result.authors(),
                result.isbn(),
                result.pageCount(),
                result.coverUrl(),
                null,
                result.externalId(),
                result.source()
        );
        return findExistingBook(request)
                .map(book -> new BookSearchResultDto(
                        book.getId(),
                        book.getTitle(),
                        List.copyOf(book.getAuthors()),
                        book.getIsbn(),
                        book.getPageCount(),
                        book.getCoverUrl(),
                        book.getSource(),
                        book.getExternalId()
                ))
                .orElse(result);
    }


}
