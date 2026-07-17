package com.whatiread.catalog.author.service;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.author.api.AuthorDto;
import com.whatiread.catalog.author.domain.Author;
import com.whatiread.catalog.author.domain.BookAuthor;
import com.whatiread.catalog.author.repository.AuthorRepository;
import com.whatiread.catalog.author.repository.BookAuthorRepository;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.service.BookMapper;
import com.whatiread.catalog.service.BookService;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.UserBook;
import com.whatiread.library.repository.UserBookRepository;
import com.whatiread.library.service.ProgressCalculator;
import com.whatiread.library.service.ProgressSnapshot;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shared.util.SlugUtils;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final UserBookRepository userBookRepository;

    public AuthorService(
            AuthorRepository authorRepository,
            BookAuthorRepository bookAuthorRepository,
            BookMapper bookMapper,
            BookService bookService,
            UserBookRepository userBookRepository
    ) {
        this.authorRepository = authorRepository;
        this.bookAuthorRepository = bookAuthorRepository;
        this.bookMapper = bookMapper;
        this.bookService = bookService;
        this.userBookRepository = userBookRepository;
    }

    @Transactional(readOnly = true)
    public AuthorDto getBySlug(String slug) {
        return toDto(findBySlug(slug));
    }

    @Transactional(readOnly = true)
    public Page<BookDto> listCatalogBooks(String slug, Pageable pageable) {
        Author author = findBySlug(slug);
        return bookAuthorRepository.findBooksByAuthorId(author.getId(), pageable)
                .map(bookMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<UserBookDto> listLibraryBooks(UUID userId, String slug, Pageable pageable) {
        Author author = findBySlug(slug);
        List<UUID> bookIds = bookAuthorRepository.findBookIdsByAuthorId(author.getId());
        if (bookIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return userBookRepository.findByUserIdAndBook_IdIn(userId, bookIds, pageable)
                .map(userBook -> toLibraryDto(userBook));
    }

    public Author findOrCreateByName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Author name is required");
        }
        return authorRepository.findByNormalizedName(trimmed)
                .orElseGet(() -> {
                    String slug = uniqueSlug(trimmed);
                    return authorRepository.save(new Author(slug, trimmed));
                });
    }

    public void linkBookAuthor(Book book, Author author, int position) {
        if (!bookAuthorRepository.existsById(new BookAuthor.BookAuthorId(book.getId(), author.getId()))) {
            bookAuthorRepository.save(new BookAuthor(book, author, position));
        }
    }

    private Author findBySlug(String slug) {
        return authorRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
    }

    private String uniqueSlug(String name) {
        String base = SlugUtils.slugify(name);
        String slug = base;
        int suffix = 2;
        while (authorRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private AuthorDto toDto(Author author) {
        return new AuthorDto(
                author.getId(),
                author.getSlug(),
                author.getName(),
                author.getBio(),
                author.getPhotoUrl(),
                author.getOpenLibraryAuthorId()
        );
    }

    private UserBookDto toLibraryDto(UserBook userBook) {
        Book book = userBook.getBook();
        ProgressSnapshot progress = ProgressCalculator.calculate(
                userBook.getStatus(),
                userBook.getProgressPages(),
                userBook.getProgressPercent(),
                book.getPageCount()
        );
        return new UserBookDto(
                userBook.getId(),
                bookService.getById(book.getId()),
                userBook.getStatus(),
                userBook.getRating(),
                progress.progressPages(),
                progress.pageCount(),
                progress.progressPercent(),
                progress.progressDisplay(),
                userBook.getStartedAt(),
                userBook.getFinishedAt(),
                List.of(),
                userBook.getCreatedAt(),
                userBook.getUpdatedAt()
        );
    }
}
