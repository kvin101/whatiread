package com.whatiread.catalog.suggest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookSuggestServiceTest {

    @Mock
    private MeilisearchBookIndexClient indexClient;

    @InjectMocks
    private BookSuggestService bookSuggestService;

    @Test
    void suggestUsesDefaultLimit() {
        when(indexClient.search("harry", 120)).thenReturn(List.of());

        bookSuggestService.suggest("harry", null);

        verify(indexClient).search("harry", 120);
    }

    @Test
    void suggestClampsLimit() {
        when(indexClient.search("dune", 150)).thenReturn(List.of());

        bookSuggestService.suggest("dune", 100);

        verify(indexClient).search("dune", 150);
    }

    @Test
    void suggestReturnsMatches() {
        List<BookSuggestDto> expected = List.of(new BookSuggestDto("Dune"));
        when(indexClient.search("dune", 120)).thenReturn(expected);

        assertThat(bookSuggestService.suggest("dune", 8)).isEqualTo(expected);
    }
}
