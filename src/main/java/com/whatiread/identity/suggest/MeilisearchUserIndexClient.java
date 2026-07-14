package com.whatiread.identity.suggest;

import com.whatiread.identity.domain.User;
import com.whatiread.search.meilisearch.MeilisearchIndexClient;
import com.whatiread.shared.util.DisplayNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MeilisearchUserIndexClient {

    private final MeilisearchIndexClient indexClient;

    public MeilisearchUserIndexClient(@Qualifier("userSuggest") MeilisearchIndexClient indexClient) {
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

    public void upsertUsers(List<User> users) {
        if (users.isEmpty()) {
            return;
        }
        ensureIndex();
        List<Map<String, Object>> documents = new ArrayList<>(users.size());
        for (User user : users) {
            documents.add(toDocument(user));
        }
        indexClient.addDocuments(documents);
    }

    public void removeUsers(List<String> userIds) {
        indexClient.deleteDocuments(userIds);
    }

    public List<IndexedUserHit> search(String query, int limit) {
        List<Map<String, Object>> hits = indexClient.search(query, limit);
        List<IndexedUserHit> results = new ArrayList<>(hits.size());
        for (Map<String, Object> hit : hits) {
            String id = text(hit.get("id"));
            String username = text(hit.get("username"));
            String displayName = text(hit.get("displayName"));
            String email = text(hit.get("email"));
            if (id.isBlank() || username.isBlank()) {
                continue;
            }
            results.add(new IndexedUserHit(id, username, displayName, email));
        }
        return results;
    }

    static Map<String, Object> toDocument(User user) {
        Map<String, Object> document = new HashMap<>();
        document.put("id", user.getId().toString());
        document.put("username", user.getUsername());
        document.put("displayName", DisplayNames.format(user));
        document.put("email", user.getEmail());
        return document;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    record IndexedUserHit(String id, String username, String displayName, String email) {
    }
}
