package com.whatiread.library.service;

import com.whatiread.library.api.AddToLibraryRequest;
import com.whatiread.library.api.CreateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookRequest;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.api.UserBookNoteDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.shared.api.CursorPage;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LibraryService {

    UserBookDto add(UUID userId, AddToLibraryRequest request);

    Page<UserBookDto> list(UUID userId, ReadingStatus status, UUID shelfId, String query, Pageable pageable);

    CursorPage<UserBookDto> listWithCursor(
            UUID userId,
            ReadingStatus status,
            UUID shelfId,
            String query,
            String cursor,
            int limit
    );

    UserBookDto ensureInLibrary(UUID userId, UUID bookId);

    UserBookDto get(UUID userId, UUID userBookId);

    UserBookDto getByBookId(UUID userId, UUID bookId);

    /**
     * Catalog + reading summary for shelf viewers; omits private notes.
     */
    UserBookDto getSharedView(UUID ownerId, UUID userBookId);

    UserBookDto update(UUID userId, UUID userBookId, UpdateUserBookRequest request);

    void delete(UUID userId, UUID userBookId);

    List<UserBookNoteDto> listNotes(UUID userId, UUID userBookId);

    UserBookNoteDto addNote(UUID userId, UUID userBookId, CreateUserBookNoteRequest request);

    UserBookNoteDto updateNote(UUID userId, UUID userBookId, UUID noteId, UpdateUserBookNoteRequest request);

    void deleteNote(UUID userId, UUID userBookId, UUID noteId);

    UUID getOwnerId(UUID userBookId);

    boolean hasBook(UUID userId, UUID bookId);

    /**
     * Returns the subset of candidate book IDs already present in the user's library.
     */
    Set<UUID> ownedBookIdsAmong(UUID userId, Collection<UUID> bookIds);

    /**
     * Batch fetch for shelf book listings; omits private notes.
     */
    List<UserBookDto> listByIds(UUID userId, java.util.Collection<UUID> userBookIds);

    List<UserBookDto> listAllForUser(UUID userId);

    /**
     * Creates or updates a library entry during import flows.
     */
    UserBookDto upsertForImport(UUID userId, UUID bookId, ReadingStatus status);

    UserBookDto updateFinishedDateIfRead(UUID userId, UUID userBookId, java.time.LocalDate finishedAt);
}
