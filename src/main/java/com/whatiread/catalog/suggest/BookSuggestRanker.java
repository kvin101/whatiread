package com.whatiread.catalog.suggest;

import com.whatiread.shared.suggest.SuggestCandidate;
import com.whatiread.shared.suggest.SuggestRanker;
import java.util.List;

final class BookSuggestRanker {

    private BookSuggestRanker() {
    }

    static List<BookSuggestDto> rerank(String query, List<BookSuggestDto> candidates, int limit) {
        List<SuggestCandidate> ranked = SuggestRanker.rerank(
                query,
                candidates.stream()
                        .map(candidate -> new SuggestCandidate(
                                candidate.title(),
                                candidate.title(),
                                SuggestRanker.tokenize(candidate.title())
                        ))
                        .toList(),
                limit
        );
        return ranked.stream().map(candidate -> new BookSuggestDto(candidate.label())).toList();
    }
}
