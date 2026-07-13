package com.whatiread.catalog.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "openLibrary", url = "https://openlibrary.org")
public interface OpenLibraryClient {

    @GetMapping("/search.json")
    OpenLibrarySearchResponse search(
            @RequestParam("q") String query,
            @RequestParam("limit") int limit,
            @RequestParam("page") int page
    );
}
