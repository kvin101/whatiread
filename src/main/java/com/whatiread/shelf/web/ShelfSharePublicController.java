package com.whatiread.shelf.web;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shelf.api.SharedShelfDto;
import com.whatiread.shelf.service.ShelfService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PUBLIC_SHELF_SHARE)
public class ShelfSharePublicController {

    private final ShelfService shelfService;

    public ShelfSharePublicController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @GetMapping("/{token}")
    SharedShelfDto get(@PathVariable UUID token) {
        return shelfService.getSharedShelf(token);
    }
}
