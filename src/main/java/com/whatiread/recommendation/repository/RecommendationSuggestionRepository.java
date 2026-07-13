package com.whatiread.recommendation.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationSuggestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationSuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findPreferredTagNames(UUID userId) {
        String sql = """
                SELECT DISTINCT t.name
                FROM tags t
                INNER JOIN user_book_tags ubt ON ubt.tag_id = t.id
                INNER JOIN user_books ub ON ub.id = ubt.user_book_id
                WHERE ub.user_id = ?
                  AND ub.rating IS NOT NULL
                  AND ub.rating >= 4
                """;
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    public List<UUID> findBookIdsBySharedTags(UUID userId, List<String> tagNames, int limit) {
        if (tagNames.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", tagNames.stream().map(name -> "?").toList());
        String sql = """
                SELECT DISTINCT ub.book_id
                FROM user_books ub
                INNER JOIN user_book_tags ubt ON ubt.user_book_id = ub.id
                INNER JOIN tags t ON t.id = ubt.tag_id
                WHERE t.name IN (%s)
                  AND ub.book_id NOT IN (
                    SELECT book_id FROM user_books WHERE user_id = ?
                  )
                LIMIT ?
                """.formatted(placeholders);
        Object[] args = new Object[tagNames.size() + 2];
        int index = 0;
        for (String tagName : tagNames) {
            args[index++] = tagName;
        }
        args[index++] = userId;
        args[index] = limit;
        return jdbcTemplate.queryForList(sql, UUID.class, args);
    }

    public List<UUID> findFriendHighlyRatedBookIds(UUID userId, int limit) {
        String sql = """
                SELECT DISTINCT ub.book_id
                FROM friendships f
                INNER JOIN user_books ub ON ub.user_id = f.friend_id
                WHERE f.user_id = ?
                  AND ub.rating IS NOT NULL
                  AND ub.rating >= 4
                  AND ub.book_id NOT IN (
                    SELECT book_id FROM user_books WHERE user_id = ?
                  )
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, UUID.class, userId, userId, limit);
    }
}
