package com.whatiread.library.service;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookWorkMatcher;
import com.whatiread.catalog.port.BookPersistencePort;
import com.whatiread.catalog.service.BookService;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.library.api.AddToLibraryRequest;
import com.whatiread.library.api.CreateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookRequest;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.api.UserBookNoteDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.domain.UserBook;
import com.whatiread.library.domain.UserBookNote;
import com.whatiread.library.port.ShelfBookQueryPort;
import com.whatiread.library.repository.UserBookNoteRepository;
import com.whatiread.library.repository.UserBookRepository;
import com.whatiread.shared.api.CursorPage;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.stream.Collectors;
import com.whatiread.shared.util.KeysetCursor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LibraryServiceImpl implements LibraryService {

    private final UserBookRepository userBookRepository;
    private final UserBookNoteRepository userBookNoteRepository;
    private final UserLookupService userLookupService;
    private final BookService bookService;
    private final BookPersistencePort bookPersistencePort;
    private final ShelfBookQueryPort shelfBookQueryPort;
    private final BusinessMetrics businessMetrics;

    public LibraryServiceImpl(
            UserBookRepository userBookRepository,
            UserBookNoteRepository userBookNoteRepository,
            UserLookupService userLookupService,
            BookService bookService,
            BookPersistencePort bookPersistencePort,
            ShelfBookQueryPort shelfBookQueryPort,
            BusinessMetrics businessMetrics
    ) {
        this.userBookRepository = userBookRepository;
        this.userBookNoteRepository = userBookNoteRepository;
        this.userLookupService = userLookupService;
        this.bookService = bookService;
        this.bookPersistencePort = bookPersistencePort;
        this.shelfBookQueryPort = shelfBookQueryPort;
        this.businessMetrics = businessMetrics;
    }

    private static List<UserBook> dedupeByWork(List<UserBook> entries) {
        Map<String, UserBook> unique = new LinkedHashMap<>();
        for (UserBook entry : entries) {
            unique.putIfAbsent(workKey(entry), entry);
        }
        return List.copyOf(unique.values());
    }

    private static String workKey(UserBook entry) {
        return workKey(entry.getBook().getTitle(), entry.getBook().getAuthors());
    }

    private static String workKey(String title, List<String> authors) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        return normalizedTitle + "|" + String.join(",", BookWorkMatcher.normalizeAuthors(authors));
    }

    private static void validateRating(BigDecimal rating) {
        BigDecimal doubled = rating.multiply(BigDecimal.valueOf(2));
        if (doubled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Rating must use 0.5 increments");
        }
        if (rating.compareTo(BigDecimal.valueOf(0.5)) < 0 || rating.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            throw new IllegalArgumentException("Rating must be between 0.5 and 5.0");
        }
    }

    @Override
    public UserBookDto add(UUID userId, AddToLibraryRequest request) {
        UUID canonicalBookId = bookService.resolveCanonicalBookId(request.bookId());
        if (findUserBookForWork(userId, canonicalBookId).isPresent()) {
            throw new ConflictException("Book already in library");
        }
        User user = userLookupService.getPersistenceReference(userId);
        Book book = bookPersistencePort.getReference(canonicalBookId);
        UserBook userBook = new UserBook(user, book, request.statusOrDefault());
        if (request.progressPages() != null) {
            userBook.setProgressPages(request.progressPages());
        }
        applyStatusSideEffects(userBook, userBook.getStatus(), userBook.getStatus());
        UserBookDto saved = toDto(userBookRepository.save(userBook));
        businessMetrics.recordBookAddedToLibrary();
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserBookDto> list(UUID userId, ReadingStatus status, UUID shelfId, String query, Pageable pageable) {
        String q = query != null ? query.trim() : "";
        Page<UserBook> page;
        List<UUID> shelfUserBookIds = shelfId != null
                ? shelfBookQueryPort.findUserBookIdsOnShelf(userId, shelfId)
                : List.of();
        if (shelfId != null && shelfUserBookIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (!q.isEmpty()) {
            if (shelfId != null) {
                page = status == null
                        ? userBookRepository.searchByUserIdAndIdIn(userId, shelfUserBookIds, q, pageable)
                        : userBookRepository.searchByUserIdAndStatusAndIdIn(userId, status, shelfUserBookIds, q, pageable);
            } else if (status == null) {
                page = userBookRepository.searchByUserId(userId, q, pageable);
            } else {
                page = userBookRepository.searchByUserIdAndStatus(userId, status, q, pageable);
            }
        } else if (shelfId != null) {
            page = status == null
                    ? userBookRepository.findByUserIdAndIdIn(userId, shelfUserBookIds, pageable)
                    : userBookRepository.findByUserIdAndStatusAndIdIn(userId, status, shelfUserBookIds, pageable);
        } else if (status == null) {
            page = userBookRepository.findByUserId(userId, pageable);
        } else {
            page = userBookRepository.findByUserIdAndStatus(userId, status, pageable);
        }
        List<UserBook> unique = dedupeByWork(page.getContent());
        long adjustedTotal = Math.max(0, page.getTotalElements() - (page.getNumberOfElements() - unique.size()));
        return new PageImpl<>(
                unique.stream().map(this::toDto).toList(),
                pageable,
                adjustedTotal
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserBookDto> listWithCursor(
            UUID userId,
            ReadingStatus status,
            UUID shelfId,
            String query,
            String cursor,
            int limit
    ) {
        if (shelfId != null || (query != null && !query.isBlank())) {
            Page<UserBookDto> page = list(
                    userId,
                    status,
                    shelfId,
                    query,
                    PageRequest.of(0, Math.max(1, limit))
            );
            return new CursorPage<>(page.getContent(), null, page.hasNext());
        }
        int pageSize = Math.min(Math.max(limit, 1), 100);
        KeysetCursor.Parts position = KeysetCursor.decode(cursor);
        List<UserBook> rows;
        if (position.updatedAt() == null) {
            rows = userBookRepository.findFirstKeysetPageByUser(
                    userId,
                    status,
                    PageRequest.of(0, pageSize + 1)
            );
        } else {
            rows = userBookRepository.findKeysetByUser(
                    userId,
                    status,
                    position.updatedAt(),
                    position.id(),
                    PageRequest.of(0, pageSize + 1)
            );
        }
        boolean hasMore = rows.size() > pageSize;
        List<UserBook> pageRows = hasMore ? rows.subList(0, pageSize) : rows;
        List<UserBookDto> items = dedupeByWork(pageRows).stream().map(this::toDto).toList();
        String nextCursor = hasMore && !pageRows.isEmpty()
                ? KeysetCursor.encode(pageRows.getLast().getUpdatedAt(), pageRows.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor, hasMore);
    }

    @Override
    public UserBookDto ensureInLibrary(UUID userId, UUID bookId) {
        UUID canonicalBookId = bookService.resolveCanonicalBookId(bookId);
        return findUserBookForWork(userId, canonicalBookId)
                .map(this::toDto)
                .orElseGet(() -> add(userId, new AddToLibraryRequest(canonicalBookId, null, null)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserBookDto get(UUID userId, UUID userBookId) {
        return toDto(findOwned(userId, userBookId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserBookDto getByBookId(UUID userId, UUID bookId) {
        UUID canonicalBookId = bookService.resolveCanonicalBookId(bookId);
        return findUserBookForWork(userId, canonicalBookId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Book not in your library"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserBookDto getSharedView(UUID ownerId, UUID userBookId) {
        UserBookDto full = toDto(findOwned(ownerId, userBookId));
        return new UserBookDto(
                full.id(),
                full.book(),
                full.status(),
                full.rating(),
                full.progressPages(),
                full.pageCount(),
                full.progressPercent(),
                full.progressDisplay(),
                full.startedAt(),
                full.finishedAt(),
                List.of(),
                full.createdAt(),
                full.updatedAt()
        );
    }

    @Override
    public UserBookDto update(UUID userId, UUID userBookId, UpdateUserBookRequest request) {
        UserBook userBook = findOwned(userId, userBookId);
        ReadingStatus previousStatus = userBook.getStatus();

        if (request.status() != null) {
            userBook.setStatus(request.status());
        }
        UUID bookId = userBook.getBook().getId();
        if (Boolean.TRUE.equals(request.clearRating())) {
            userBook.setRating(null);
        } else if (request.rating() != null) {
            validateRating(request.rating());
            userBook.setRating(request.rating());
        }
        if (request.progressPages() != null) {
            userBook.setProgressPages(request.progressPages());
        }
        if (request.progressPercent() != null) {
            userBook.setProgressPercent(request.progressPercent());
        }
        if (request.startedAt() != null) {
            userBook.setStartedAt(request.startedAt());
        }
        if (request.finishedAt() != null) {
            userBook.setFinishedAt(request.finishedAt());
        }

        applyStatusSideEffects(userBook, previousStatus, userBook.getStatus());
        syncStoredPercent(userBook);
        UserBook saved = userBookRepository.save(userBook);
        if (Boolean.TRUE.equals(request.clearRating()) || request.rating() != null) {
            bookService.refreshAggregatedRating(bookId);
        }
        return toDto(saved);
    }

    @Override
    public void delete(UUID userId, UUID userBookId) {
        UserBook userBook = findOwned(userId, userBookId);
        UUID bookId = userBook.getBook().getId();
        boolean hadRating = userBook.getRating() != null;
        userBookRepository.delete(userBook);
        if (hadRating) {
            bookService.refreshAggregatedRating(bookId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBookNoteDto> listNotes(UUID userId, UUID userBookId) {
        findOwned(userId, userBookId);
        return userBookNoteRepository.findByUserBookIdOrderByCreatedAtAsc(userBookId).stream()
                .map(this::toNoteDto)
                .toList();
    }

    @Override
    public UserBookNoteDto addNote(UUID userId, UUID userBookId, CreateUserBookNoteRequest request) {
        UserBook userBook = findOwned(userId, userBookId);
        User author = userLookupService.getPersistenceReference(userId);
        UserBookNote note = new UserBookNote(userBook, author, request.body().trim());
        userBook.addNote(note);
        return toNoteDto(userBookNoteRepository.save(note));
    }

    @Override
    public UserBookNoteDto updateNote(UUID userId, UUID userBookId, UUID noteId, UpdateUserBookNoteRequest request) {
        findOwned(userId, userBookId);
        UserBookNote note = userBookNoteRepository.findByIdAndUserBook_IdAndUserBook_User_Id(noteId, userBookId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
        note.setBody(request.body().trim());
        return toNoteDto(userBookNoteRepository.save(note));
    }

    @Override
    public void deleteNote(UUID userId, UUID userBookId, UUID noteId) {
        findOwned(userId, userBookId);
        UserBookNote note = userBookNoteRepository.findByIdAndUserBook_IdAndUserBook_User_Id(noteId, userBookId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));
        userBookNoteRepository.delete(note);
        userBookNoteRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getOwnerId(UUID userBookId) {
        return userBookRepository.findById(userBookId)
                .map(userBook -> userBook.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBook(UUID userId, UUID bookId) {
        UUID canonicalBookId = bookService.resolveCanonicalBookId(bookId);
        return findUserBookForWork(userId, canonicalBookId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> ownedBookIdsAmong(UUID userId, Collection<UUID> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Set.of();
        }
        Map<UUID, UUID> canonicalByBookId = new LinkedHashMap<>();
        for (UUID bookId : bookIds) {
            canonicalByBookId.putIfAbsent(bookId, bookService.resolveCanonicalBookId(bookId));
        }
        Set<UUID> ownedCanonicalIds = userBookRepository.findOwnedBookIdsByUserIdAndBookIdIn(
                userId,
                canonicalByBookId.values()
        );
        Set<String> ownedWorkKeys = null;
        Set<UUID> owned = new HashSet<>();
        for (var entry : canonicalByBookId.entrySet()) {
            if (ownedCanonicalIds.contains(entry.getValue())) {
                owned.add(entry.getKey());
                continue;
            }
            if (ownedWorkKeys == null) {
                ownedWorkKeys = userBookRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                        .map(LibraryServiceImpl::workKey)
                        .collect(Collectors.toSet());
            }
            Book reference = bookPersistencePort.getReference(entry.getValue());
            if (ownedWorkKeys.contains(workKey(reference.getTitle(), reference.getAuthors()))) {
                owned.add(entry.getKey());
            }
        }
        return owned;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBookDto> listByIds(UUID userId, Collection<UUID> userBookIds) {
        if (userBookIds == null || userBookIds.isEmpty()) {
            return List.of();
        }
        return userBookRepository.findOwnedByUserIdAndIdIn(userId, userBookIds).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBookDto> listAllForUser(UUID userId) {
        return userBookRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public UserBookDto upsertForImport(UUID userId, UUID bookId, ReadingStatus status) {
        UUID canonicalBookId = bookService.resolveCanonicalBookId(bookId);
        Optional<UserBook> existing = findUserBookForWork(userId, canonicalBookId);
        if (existing.isPresent()) {
            UserBook userBook = existing.get();
            ReadingStatus previous = userBook.getStatus();
            userBook.setStatus(status);
            applyStatusSideEffects(userBook, previous, status);
            return toDto(userBookRepository.save(userBook));
        }
        return add(userId, new AddToLibraryRequest(canonicalBookId, status, null));
    }

    @Override
    public UserBookDto updateFinishedDateIfRead(UUID userId, UUID userBookId, LocalDate finishedAt) {
        UserBook userBook = findOwned(userId, userBookId);
        if (finishedAt != null && userBook.getStatus() == ReadingStatus.READ) {
            userBook.setFinishedAt(finishedAt);
            return toDto(userBookRepository.save(userBook));
        }
        return toDto(userBook);
    }

    private Optional<UserBook> findUserBookForWork(UUID userId, UUID canonicalBookId) {
        Optional<UserBook> direct = userBookRepository.findByUserIdAndBook_Id(userId, canonicalBookId);
        if (direct.isPresent()) {
            return direct;
        }
        Book reference = bookPersistencePort.getReference(canonicalBookId);
        return userBookRepository.findByUserIdAndNormalizedBookTitle(userId, reference.getTitle()).stream()
                .filter(userBook -> BookWorkMatcher.sameWork(
                        reference.getTitle(),
                        reference.getAuthors(),
                        userBook.getBook().getTitle(),
                        userBook.getBook().getAuthors()
                ))
                .findFirst();
    }

    private UserBook findOwned(UUID userId, UUID userBookId) {
        return userBookRepository.findByIdAndUserId(userBookId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
    }

    private void applyStatusSideEffects(UserBook userBook, ReadingStatus previous, ReadingStatus current) {
        if (current == ReadingStatus.READING && userBook.getStartedAt() == null) {
            userBook.setStartedAt(LocalDate.now());
        }
        if (current == ReadingStatus.READ && userBook.getFinishedAt() == null) {
            userBook.setFinishedAt(LocalDate.now());
        }
        if (previous == ReadingStatus.READ && current != ReadingStatus.READ) {
            userBook.setFinishedAt(null);
        }
    }

    private void syncStoredPercent(UserBook userBook) {
        ProgressSnapshot snapshot = ProgressCalculator.calculate(
                userBook.getStatus(),
                userBook.getProgressPages(),
                userBook.getProgressPercent(),
                userBook.getBook().getPageCount()
        );
        userBook.setProgressPercent(snapshot.progressPercent());
    }

    private UserBookDto toSummaryDto(UserBook userBook) {
        Book book = userBook.getBook();
        ProgressSnapshot progress = ProgressCalculator.calculate(
                userBook.getStatus(),
                userBook.getProgressPages(),
                userBook.getProgressPercent(),
                book.getPageCount()
        );
        BookDto bookDto = bookService.getById(book.getId());
        return new UserBookDto(
                userBook.getId(),
                bookDto,
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

    private UserBookDto toDto(UserBook userBook) {
        Book book = userBook.getBook();
        ProgressSnapshot progress = ProgressCalculator.calculate(
                userBook.getStatus(),
                userBook.getProgressPages(),
                userBook.getProgressPercent(),
                book.getPageCount()
        );
        BookDto bookDto = bookService.getById(book.getId());
        List<UserBookNoteDto> notes = userBook.getNotes().stream()
                .map(this::toNoteDto)
                .toList();
        return new UserBookDto(
                userBook.getId(),
                bookDto,
                userBook.getStatus(),
                userBook.getRating(),
                progress.progressPages(),
                progress.pageCount(),
                progress.progressPercent(),
                progress.progressDisplay(),
                userBook.getStartedAt(),
                userBook.getFinishedAt(),
                notes,
                userBook.getCreatedAt(),
                userBook.getUpdatedAt()
        );
    }

    private UserBookNoteDto toNoteDto(UserBookNote note) {
        return new UserBookNoteDto(
                note.getId(),
                note.getBody(),
                note.getAuthor().getId(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
