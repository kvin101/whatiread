package com.whatiread.catalog.suggest;

import com.whatiread.search.meilisearch.MeilisearchIndexClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MeilisearchBookIndexClient {

    private final MeilisearchIndexClient indexClient;

    public MeilisearchBookIndexClient(@Qualifier("bookSuggest") MeilisearchIndexClient indexClient) {
        this.indexClient = indexClient;
    }

    public boolean isEnabled() {
        return indexClient.isEnabled();
    }

    public long documentCount() {
        return indexClient.documentCount();
    }

    public void ensureIndex() {
        indexClient.ensureIndex();
    }

    public void addDocuments(List<Map<String, Object>> documents) {
        indexClient.addDocuments(documents);
    }

    public List<BookSuggestDto> search(String query, int limit) {
        List<Map<String, Object>> hits = indexClient.search(query, limit);
        List<BookSuggestDto> results = new ArrayList<>(hits.size());
        for (Map<String, Object> hit : hits) {
            String title = text(hit.get("title"));
            if (title.isBlank()) {
                continue;
            }
            results.add(new BookSuggestDto(title));
        }
        return results;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
