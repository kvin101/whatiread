package com.whatiread.config;

import com.whatiread.search.meilisearch.MeilisearchIndexClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MeilisearchConfig {

    @Bean
    @Qualifier("bookSuggest")
    MeilisearchIndexClient bookSuggestIndexClient(
            MeilisearchProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        return new MeilisearchIndexClient(
                properties,
                restClientBuilder,
                properties.index(),
                List.of("title"),
                List.of("title")
        );
    }

    @Bean
    @Qualifier("userSuggest")
    MeilisearchIndexClient userSuggestIndexClient(
            MeilisearchProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        return new MeilisearchIndexClient(
                properties,
                restClientBuilder,
                properties.userIndex(),
                List.of("username", "displayName", "email"),
                List.of("id", "username", "displayName", "email")
        );
    }
}
