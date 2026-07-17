package com.whatiread.library.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.library.api.AddToLibraryRequest;
import com.whatiread.library.api.CreateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookNoteRequest;
import com.whatiread.library.api.UpdateUserBookRequest;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.api.UserBookNoteDto;
import com.whatiread.library.domain.LibrarySort;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.library.service.LibraryService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.LIBRARY)
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserBookDto add(@CurrentUserId UUID userId, @Valid @RequestBody AddToLibraryRequest request) {
        return libraryService.add(userId, request);
    }

    @GetMapping
    Object list(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) ReadingStatus status,
            @RequestParam(required = false) UUID shelfId,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LibrarySort sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        LibrarySort effectiveSort = sort != null ? sort : LibrarySort.UPDATED_DESC;
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        if (cursor != null) {
            return libraryService.listWithCursor(userId, status, shelfId, authorId, q, effectiveSort, cursor, limit);
        }
        return libraryService.list(userId, status, shelfId, authorId, q, effectiveSort, paging);
    }

    @GetMapping("/by-book/{bookId}")
    UserBookDto getByBook(@CurrentUserId UUID userId, @PathVariable UUID bookId) {
        return libraryService.getByBookId(userId, bookId);
    }

    @GetMapping("/{userBookId}")
    UserBookDto get(@CurrentUserId UUID userId, @PathVariable UUID userBookId) {
        return libraryService.get(userId, userBookId);
    }

    @PatchMapping("/{userBookId}")
    UserBookDto update(
            @CurrentUserId UUID userId,
            @PathVariable UUID userBookId,
            @Valid @RequestBody UpdateUserBookRequest request
    ) {
        return libraryService.update(userId, userBookId, request);
    }

    @DeleteMapping("/{userBookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId UUID userId, @PathVariable UUID userBookId) {
        libraryService.delete(userId, userBookId);
    }

    @GetMapping("/{userBookId}/notes")
    List<UserBookNoteDto> listNotes(@CurrentUserId UUID userId, @PathVariable UUID userBookId) {
        return libraryService.listNotes(userId, userBookId);
    }

    @PostMapping("/{userBookId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    UserBookNoteDto addNote(
            @CurrentUserId UUID userId,
            @PathVariable UUID userBookId,
            @Valid @RequestBody CreateUserBookNoteRequest request
    ) {
        return libraryService.addNote(userId, userBookId, request);
    }

    @PatchMapping("/{userBookId}/notes/{noteId}")
    UserBookNoteDto updateNote(
            @CurrentUserId UUID userId,
            @PathVariable UUID userBookId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateUserBookNoteRequest request
    ) {
        return libraryService.updateNote(userId, userBookId, noteId, request);
    }

    @DeleteMapping("/{userBookId}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteNote(@CurrentUserId UUID userId, @PathVariable UUID userBookId, @PathVariable UUID noteId) {
        libraryService.deleteNote(userId, userBookId, noteId);
    }
}
