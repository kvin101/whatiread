package com.whatiread.catalog.suggest;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookSuggestService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;
    private static final int CANDIDATE_MULTIPLIER = 8;
    private static final int MAX_CANDIDATES = 150;

    private final MeilisearchBookIndexClient indexClient;

    public BookSuggestService(MeilisearchBookIndexClient indexClient) {
        this.indexClient = indexClient;
    }

    public List<BookSuggestDto> suggest(String query, Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        int candidateLimit = candidatePoolSize(query, resolvedLimit);
        List<BookSuggestDto> candidates = indexClient.search(query, candidateLimit);
        return BookSuggestRanker.rerank(query, candidates, resolvedLimit);
    }

    private static int candidatePoolSize(String query, int resolvedLimit) {
        int trimmedLength = query == null ? 0 : query.trim().length();
        int minimum = trimmedLength <= 5 ? 120 : 40;
        return Math.min(MAX_CANDIDATES, Math.max(resolvedLimit * CANDIDATE_MULTIPLIER, minimum));
    }
}
