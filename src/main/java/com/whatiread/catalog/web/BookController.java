package com.whatiread.catalog.web;

import com.whatiread.catalog.api.BookDto;
import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.api.CreateBookRequest;
import com.whatiread.catalog.service.BookService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BOOKS)
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/search")
    Page<BookSearchResultDto> search(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return bookService.search(query, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookDto create(@Valid @RequestBody CreateBookRequest request) {
        return bookService.createManual(request);
    }

    @GetMapping("/{bookId}")
    BookDto get(@PathVariable UUID bookId) {
        return bookService.getById(bookId);
    }
}
