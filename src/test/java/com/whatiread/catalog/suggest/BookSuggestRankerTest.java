package com.whatiread.catalog.suggest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BookSuggestRankerTest {

    @Test
    void rerankPrefersPrefixMatches() {
        List<BookSuggestDto> ranked = BookSuggestRanker.rerank(
                "har",
                List.of(
                        new BookSuggestDto("Harbinger"),
                        new BookSuggestDto("Harry Potter and the Chamber of Secrets")
                ),
                2
        );

        assertThat(ranked.getFirst().title()).isEqualTo("Harry Potter and the Chamber of Secrets");
    }

    @Test
    void rerankHandlesTypos() {
        List<BookSuggestDto> ranked = BookSuggestRanker.rerank(
                "hryy",
                List.of(
                        new BookSuggestDto("花樣年華 HYYH The Notes 1"),
                        new BookSuggestDto("Harry Potter and the Chamber of Secrets")
                ),
                2
        );

        assertThat(ranked.getFirst().title()).contains("Harry Potter");
    }

    @Test
    void rerankHandlesMissingLetters() {
        List<BookSuggestDto> ranked = BookSuggestRanker.rerank(
                "nruto",
                List.of(
                        new BookSuggestDto("Nauti Dreams"),
                        new BookSuggestDto("Naruto, Vol. 01: Uzumaki Naruto")
                ),
                2
        );

        assertThat(ranked.getFirst().title()).contains("Naruto");
    }

    @Test
    void rerankHandlesSingleLetterTypos() {
        List<BookSuggestDto> ranked = BookSuggestRanker.rerank(
                "hary",
                List.of(
                        new BookSuggestDto("Hairy And Slug"),
                        new BookSuggestDto("Hard as It Gets"),
                        new BookSuggestDto("Harry Potter and the Chamber of Secrets")
                ),
                3
        );

        assertThat(ranked.getFirst().title()).contains("Harry Potter");
    }
}
