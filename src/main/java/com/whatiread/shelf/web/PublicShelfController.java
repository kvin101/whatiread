package com.whatiread.shelf.web;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shelf.api.ShelfBookDto;
import com.whatiread.shelf.api.ShelfDto;
import com.whatiread.shelf.service.ShelfService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PUBLIC_USER_SHELVES)
public class PublicShelfController {

    private final ShelfService shelfService;

    public PublicShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @GetMapping
    List<ShelfDto> listPublic(@PathVariable UUID ownerId) {
        return shelfService.listPublicByOwner(ownerId);
    }

    @GetMapping("/{slug}")
    ShelfDto getPublic(@PathVariable UUID ownerId, @PathVariable String slug) {
        return shelfService.getPublic(ownerId, slug);
    }

    @GetMapping("/{slug}/books")
    List<ShelfBookDto> listPublicBooks(@PathVariable UUID ownerId, @PathVariable String slug) {
        return shelfService.listPublicBooks(ownerId, slug);
    }
}
