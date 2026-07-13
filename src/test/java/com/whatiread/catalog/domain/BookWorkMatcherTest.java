package com.whatiread.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BookWorkMatcherTest {


    private static final String TITLE = "Title";
    private static final String AUTHOR_A = "Author A";
    private static final String AUTHOR_B = "Author B";
    private static final String UNKNOWN = "unknown";

    @Test
    void normalizeAuthors_nullOrEmpty_returnsUnknown() {
        assertThat(BookWorkMatcher.normalizeAuthors(null)).containsExactly(UNKNOWN);
        assertThat(BookWorkMatcher.normalizeAuthors(List.of())).containsExactly(UNKNOWN);
    }

    @Test
    void normalizeAuthors_filtersBlanksAndLowercasesAndSorts() {
        List<String> input = List.of("  Zebra  ", "", " ", "Apple");
        assertThat(BookWorkMatcher.normalizeAuthors(input)).containsExactly("apple", "zebra");
    }

    @Test
    void authorsMatch_sameNormalized_returnsTrue() {
        List<String> list1 = List.of(AUTHOR_A, AUTHOR_B);
        List<String> list2 = List.of("author b", "author a");
        assertThat(BookWorkMatcher.authorsMatch(list1, list2)).isTrue();
    }

    @Test
    void authorsMatch_differentNormalized_returnsFalse() {
        List<String> list1 = List.of(AUTHOR_A);
        List<String> list2 = List.of(AUTHOR_B);
        assertThat(BookWorkMatcher.authorsMatch(list1, list2)).isFalse();
    }

    @Test
    void sameTitle_nullOrEmpty_returnsFalse() {
        assertThat(BookWorkMatcher.sameTitle(null, TITLE)).isFalse();
        assertThat(BookWorkMatcher.sameTitle(TITLE, "")).isFalse();
    }

    @Test
    void sameTitle_caseInsensitiveMatch_returnsTrue() {
        assertThat(BookWorkMatcher.sameTitle("  The Title  ", "the title")).isTrue();
    }

    @Test
    void sameWork_matchesTitleAndAuthors_returnsTrue() {
        assertThat(BookWorkMatcher.sameWork(
                TITLE, List.of("Author"),
                "title", List.of("author")
        )).isTrue();
    }

    @Test
    void sameWork_differentTitle_returnsFalse() {
        assertThat(BookWorkMatcher.sameWork(
                "Title A", List.of("Author"),
                "Title B", List.of("Author")
        )).isFalse();
    }

    @Test
    void sameWork_differentAuthors_returnsFalse() {
        assertThat(BookWorkMatcher.sameWork(
                TITLE, List.of(AUTHOR_A),
                TITLE, List.of(AUTHOR_B)
        )).isFalse();
    }
}
