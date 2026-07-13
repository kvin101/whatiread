package com.whatiread.shelf.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shelf.api.CreateShelfShareLinkRequest;
import com.whatiread.shelf.api.ShelfShareLinkDto;
import com.whatiread.shelf.service.ShelfService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SHELVES + "/{shelfId}/share-links")
public class ShelfShareLinkController {

    private final ShelfService shelfService;

    public ShelfShareLinkController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @GetMapping
    List<ShelfShareLinkDto> list(@CurrentUserId UUID userId, @PathVariable UUID shelfId) {
        return shelfService.listShareLinks(userId, shelfId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShelfShareLinkDto create(
            @CurrentUserId UUID userId,
            @PathVariable UUID shelfId,
            @Valid @RequestBody(required = false) CreateShelfShareLinkRequest request
    ) {
        return shelfService.createShareLink(userId, shelfId, request);
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@CurrentUserId UUID userId, @PathVariable UUID shelfId, @PathVariable UUID linkId) {
        shelfService.revokeShareLink(userId, shelfId, linkId);
    }
}
