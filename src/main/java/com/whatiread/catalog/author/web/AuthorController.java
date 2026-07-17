package com.whatiread.catalog.author.web;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.author.api.AuthorDto;
import com.whatiread.catalog.author.service.AuthorService;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.library.api.UserBookDto;
import com.whatiread.shared.web.ApiPaths;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTHORS)
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("/{slug}")
    AuthorDto get(@PathVariable String slug) {
        return authorService.getBySlug(slug);
    }

    @GetMapping("/{slug}/books")
    Page<BookDto> listBooks(@PathVariable String slug, @PageableDefault(size = 20) Pageable pageable) {
        return authorService.listCatalogBooks(slug, pageable);
    }

    @GetMapping("/{slug}/library")
    Page<UserBookDto> listLibraryBooks(
            @PathVariable String slug,
            @CurrentUserId UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return authorService.listLibraryBooks(userId, slug, pageable);
    }
}
