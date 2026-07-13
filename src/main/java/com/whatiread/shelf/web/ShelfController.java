package com.whatiread.shelf.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.library.domain.ReadingStatus;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shelf.api.AddShelfBookRequest;
import com.whatiread.shelf.api.AddShelfMemberRequest;
import com.whatiread.shelf.api.CloneShelfRequest;
import com.whatiread.shelf.api.CreateShelfRequest;
import com.whatiread.shelf.api.ExploreShelfDto;
import com.whatiread.shelf.api.ShelfBookDto;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.api.ShelfEventDto;
import com.whatiread.shelf.api.ShelfMemberDto;
import com.whatiread.shelf.api.SystemShelfDto;
import com.whatiread.shelf.api.UpdateShelfBookRequest;
import com.whatiread.shelf.api.UpdateShelfMemberRequest;
import com.whatiread.shelf.api.UpdateShelfRequest;
import com.whatiread.shelf.service.ShelfService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SHELVES)
public class ShelfController {

    private final ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShelfDto create(@CurrentUserId UUID userId, @Valid @RequestBody CreateShelfRequest request) {
        return shelfService.create(userId, request);
    }

    @GetMapping
    List<ShelfDto> listMine(@CurrentUserId UUID userId) {
        return shelfService.listMine(userId);
    }

    @GetMapping("/explore")
    Page<ExploreShelfDto> explore(@CurrentUserId UUID userId, @PageableDefault(size = 24) Pageable pageable) {
        return shelfService.exploreFeed(userId, pageable);
    }

    @GetMapping("/system")
    List<SystemShelfDto> listSystemShelves() {
        return shelfService.listSystemShelves();
    }

    @GetMapping("/system/{status}/books")
    Page<UserBookDto> listSystemShelfBooks(
            @CurrentUserId UUID userId,
            @PathVariable ReadingStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return shelfService.listSystemShelfBooks(userId, status, pageable);
    }

    @GetMapping("/{shelfId}")
    ShelfDto get(@CurrentUserId UUID userId, @PathVariable UUID shelfId) {
        return shelfService.get(userId, shelfId);
    }

    @PatchMapping("/{shelfId}")
    ShelfDto update(@CurrentUserId UUID userId, @PathVariable UUID shelfId, @Valid @RequestBody UpdateShelfRequest request) {
        return shelfService.update(userId, shelfId, request);
    }

    @DeleteMapping("/{shelfId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId UUID userId, @PathVariable UUID shelfId) {
        shelfService.delete(userId, shelfId);
    }

    @GetMapping("/{shelfId}/members")
    List<ShelfMemberDto> listMembers(@CurrentUserId UUID userId, @PathVariable UUID shelfId) {
        return shelfService.listMembers(userId, shelfId);
    }

    @PostMapping("/{shelfId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    ShelfMemberDto addMember(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @Valid @RequestBody AddShelfMemberRequest request
    ) {
        return shelfService.addMember(userId, shelfId, request);
    }

    @PatchMapping("/{shelfId}/members/{memberUserId}")
    ShelfMemberDto updateMember(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @PathVariable UUID memberUserId,
            @Valid @RequestBody UpdateShelfMemberRequest request
    ) {
        return shelfService.updateMember(userId, shelfId, memberUserId, request);
    }

    @DeleteMapping("/{shelfId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(@CurrentUserId UUID userId, @PathVariable UUID shelfId, @PathVariable UUID memberUserId) {
        shelfService.removeMember(userId, shelfId, memberUserId);
    }

    @GetMapping("/{shelfId}/events")
    Page<ShelfEventDto> listEvents(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        return shelfService.listEvents(userId, shelfId, pageable);
    }

    @PostMapping("/{shelfId}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    ShelfDto cloneShelf(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @Valid @RequestBody CloneShelfRequest request
    ) {
        return shelfService.cloneShelf(userId, shelfId, request);
    }

    @PostMapping("/share/{token}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    ShelfDto cloneFromShare(
            @CurrentUserId UUID userId,
            @PathVariable UUID token,
            @Valid @RequestBody CloneShelfRequest request
    ) {
        return shelfService.cloneFromShare(userId, token, request);
    }

    @GetMapping("/{shelfId}/books")
    List<ShelfBookDto> listBooks(@CurrentUserId UUID userId, @PathVariable UUID shelfId) {
        return shelfService.listBooks(userId, shelfId);
    }

    @PostMapping("/{shelfId}/books")
    @ResponseStatus(HttpStatus.CREATED)
    ShelfBookDto addBook(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @Valid @RequestBody AddShelfBookRequest request
    ) {
        return shelfService.addBook(userId, shelfId, request);
    }

    @PatchMapping("/{shelfId}/books/{userBookId}")
    ShelfBookDto updateBook(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @PathVariable UUID userBookId,
            @Valid @RequestBody UpdateShelfBookRequest request
    ) {
        return shelfService.updateBook(userId, shelfId, userBookId, request);
    }

    @DeleteMapping("/{shelfId}/books/{userBookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeBook(@CurrentUserId UUID userId, @PathVariable UUID shelfId, @PathVariable UUID userBookId) {
        shelfService.removeBook(userId, shelfId, userBookId);
    }
}
