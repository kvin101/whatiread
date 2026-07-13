package com.whatiread.catalog.domain;

import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class BookWorkMatcher {

    private BookWorkMatcher() {
    }

    public static List<String> normalizeAuthors(List<String> authors) {
        if (authors == null || authors.isEmpty()) {
            return List.of("unknown");
        }
        return authors.stream()
                .filter(StringUtils::hasText)
                .map(a -> a.trim().toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
    }

    public static boolean authorsMatch(List<String> existing, List<String> incoming) {
        return normalizeAuthors(existing).equals(normalizeAuthors(incoming));
    }

    public static boolean sameTitle(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    public static boolean sameWork(String title, List<String> authors, String otherTitle, List<String> otherAuthors) {
        return sameTitle(title, otherTitle) && authorsMatch(authors, otherAuthors);
    }
}
