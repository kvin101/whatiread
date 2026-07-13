package com.whatiread.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RecommendationSuggestionRepositoryTest {

    private static final String SCI_FI_TAG = "Sci-Fi";
    private static final String FANTASY_TAG = "Fantasy";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RecommendationSuggestionRepository repository;

    @Test
    void findPreferredTagNamesQueriesRatedTags() {
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(userId)))
                .thenReturn(List.of(SCI_FI_TAG, FANTASY_TAG));

        assertThat(repository.findPreferredTagNames(userId)).containsExactly(SCI_FI_TAG, FANTASY_TAG);
    }

    @Test
    void findBookIdsBySharedTagsReturnsEmptyForNoTags() {
        assertThat(repository.findBookIdsBySharedTags(UUID.randomUUID(), List.of(), 10)).isEmpty();
    }

    @Test
    void findBookIdsBySharedTagsBuildsDynamicInClause() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(any(String.class), eq(UUID.class), any(Object[].class)))
                .thenReturn(List.of(bookId));

        List<UUID> results = repository.findBookIdsBySharedTags(userId, List.of(SCI_FI_TAG, FANTASY_TAG), 5);

        assertThat(results).containsExactly(bookId);
        verify(jdbcTemplate).queryForList(any(String.class), eq(UUID.class), any(Object[].class));
    }

    @Test
    void findFriendHighlyRatedBookIdsDelegatesToJdbc() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(any(String.class), eq(UUID.class), eq(userId), eq(userId), eq(8)))
                .thenReturn(List.of(bookId));

        assertThat(repository.findFriendHighlyRatedBookIds(userId, 8)).containsExactly(bookId);
    }
}
