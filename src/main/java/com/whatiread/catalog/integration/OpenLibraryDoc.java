package com.whatiread.catalog.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryDoc(
        String title,
        @JsonProperty("author_name") List<String> authorName,
        List<String> isbn,
        @JsonProperty("cover_i") Long coverId,
        @JsonProperty("number_of_pages_median") Integer pageCount,
        String key
) {
}
