package com.whatiread.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "whatiread.meilisearch")
public record MeilisearchProperties(
        boolean enabled,
        String host,
        String apiKey,
        String index,
        String userIndex,
        String seedResource,
        boolean loadSeedOnStartup,
        boolean loadUsersOnStartup
) {

    public MeilisearchProperties {
        if (host == null || host.isBlank()) {
            host = "http://localhost:7700";
        }
        if (index == null || index.isBlank()) {
            index = "book-suggest";
        }
        if (userIndex == null || userIndex.isBlank()) {
            userIndex = "user-suggest";
        }
        if (seedResource == null || seedResource.isBlank()) {
            seedResource = "classpath:book-suggest/book-suggest.json";
        }
    }
}
