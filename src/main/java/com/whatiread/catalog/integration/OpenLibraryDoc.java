package com.whatiread.catalog.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryDoc(
        String title,
        String subtitle,
        @JsonProperty("author_name") List<String> authorName,
        List<String> isbn,
        @JsonProperty("cover_i") Long coverId,
        @JsonProperty("cover_edition_key") String coverEditionKey,
        @JsonProperty("number_of_pages_median") Integer pageCount,
        @JsonProperty("first_publish_year") Integer firstPublishYear,
        String key
) {
}
