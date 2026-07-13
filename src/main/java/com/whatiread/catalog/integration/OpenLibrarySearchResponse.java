package com.whatiread.catalog.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibrarySearchResponse(
        List<OpenLibraryDoc> docs,
        @JsonProperty("num_found") long numFound
) {
}
