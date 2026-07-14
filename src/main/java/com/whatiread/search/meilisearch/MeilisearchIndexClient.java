package com.whatiread.search.meilisearch;

import com.whatiread.config.MeilisearchProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class MeilisearchIndexClient {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchIndexClient.class);

    private final MeilisearchProperties properties;
    private final RestClient restClient;
    private final String indexUid;
    private final List<String> searchableAttributes;
    private final List<String> attributesToRetrieve;

    public MeilisearchIndexClient(
            MeilisearchProperties properties,
            RestClient.Builder restClientBuilder,
            String indexUid,
            List<String> searchableAttributes,
            List<String> attributesToRetrieve
    ) {
        this.properties = properties;
        this.indexUid = indexUid;
        this.searchableAttributes = searchableAttributes;
        this.attributesToRetrieve = attributesToRetrieve;
        RestClient.Builder builder = restClientBuilder.baseUrl(properties.host());
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder = builder.defaultHeader("Authorization", "Bearer " + properties.apiKey());
        }
        this.restClient = builder.build();
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public String indexUid() {
        return indexUid;
    }

    public long documentCount() {
        if (!properties.enabled()) {
            return 0;
        }
        try {
            MeilisearchStatsResponse stats = restClient.get()
                    .uri("/indexes/{index}/stats", indexUid)
                    .retrieve()
                    .body(MeilisearchStatsResponse.class);
            return stats != null ? stats.numberOfDocuments() : 0;
        } catch (RestClientException ex) {
            log.warn("Could not read Meilisearch stats for index '{}': {}", indexUid, ex.getMessage());
            return 0;
        }
    }

    public void ensureIndex() {
        if (!properties.enabled()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/indexes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("uid", indexUid, "primaryKey", "id"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.debug("Meilisearch index '{}' may already exist: {}", indexUid, ex.getMessage());
        }

        restClient.patch()
                .uri("/indexes/{index}/settings", indexUid)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "searchableAttributes", searchableAttributes,
                        "rankingRules", List.of("words", "typo", "proximity", "exactness", "attribute", "sort"),
                        "typoTolerance", Map.of(
                                "enabled", true,
                                "minWordSizeForTypos", Map.of(
                                        "oneTypo", 3,
                                        "twoTypos", 4
                                )
                        )
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void addDocuments(List<Map<String, Object>> documents) {
        if (!properties.enabled() || documents.isEmpty()) {
            return;
        }
        restClient.post()
                .uri("/indexes/{index}/documents", indexUid)
                .contentType(MediaType.APPLICATION_JSON)
                .body(documents)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteDocuments(List<String> documentIds) {
        if (!properties.enabled() || documentIds.isEmpty()) {
            return;
        }
        restClient.post()
                .uri("/indexes/{index}/documents/delete-batch", indexUid)
                .contentType(MediaType.APPLICATION_JSON)
                .body(documentIds)
                .retrieve()
                .toBodilessEntity();
    }

    public List<Map<String, Object>> search(String query, int limit) {
        if (!properties.enabled() || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            MeilisearchSearchResponse response = restClient.post()
                    .uri("/indexes/{index}/search", indexUid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "q", query.trim(),
                            "limit", limit,
                            "attributesToRetrieve", attributesToRetrieve
                    ))
                    .retrieve()
                    .body(MeilisearchSearchResponse.class);
            if (response == null || response.hits() == null) {
                return List.of();
            }
            return response.hits();
        } catch (RestClientException ex) {
            log.warn("Meilisearch search failed for index '{}': {}", indexUid, ex.getMessage());
            return List.of();
        }
    }

    private record MeilisearchStatsResponse(long numberOfDocuments) {
    }

    private record MeilisearchSearchResponse(List<Map<String, Object>> hits) {
    }
}
