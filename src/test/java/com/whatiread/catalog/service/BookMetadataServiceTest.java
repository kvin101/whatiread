package com.whatiread.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookSearchResultDto;
import com.whatiread.catalog.domain.Book;
import com.whatiread.catalog.domain.BookSource;
import com.whatiread.catalog.integration.OpenLibraryClient;
import com.whatiread.catalog.integration.OpenLibraryDoc;
import com.whatiread.catalog.integration.OpenLibrarySearchResponse;
import com.whatiread.config.CacheConfig;
import com.whatiread.config.observability.DependencyMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ExtendWith(MockitoExtension.class)
class BookMetadataServiceTest {


    private static final String WILLIAM_GIBSON = "William Gibson";
    private static final String FRANK_HERBERT = "Frank Herbert";
    private static final String NEUROMANCER = "Neuromancer";
    private static final String EMPTY = "empty";
    private static final String DUNE = "Dune";
    private static final String DUNE_2 = "dune";
    @Mock
    private OpenLibraryClient openLibraryClient;

    @InjectMocks
    private BookMetadataService bookMetadataService;

    @Test
    void searchExternalMapsOpenLibraryResults() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                DUNE,
                List.of(FRANK_HERBERT),
                List.of("9780441172719", "0441172717"),
                9266751L,
                688,
                "/works/OL893479W"
        );
        when(openLibraryClient.search(eq(DUNE_2), eq(10), eq(1)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(doc), 1));

        List<BookSearchResultDto> results = bookMetadataService.searchExternal(DUNE_2, 0, 10);

        assertEquals(1, results.size());
        BookSearchResultDto result = results.getFirst();
        assertEquals(DUNE, result.title());
        assertEquals(List.of(FRANK_HERBERT), result.authors());
        assertEquals("9780441172719", result.isbn());
        assertEquals(688, result.pageCount());
        assertEquals("https://covers.openlibrary.org/b/id/9266751-M.jpg", result.coverUrl());
    }

    @Test
    void extractIsbnPrefersIsbn13() {
        assertEquals("9780441172719", BookMetadataService.extractIsbn(List.of("0441172717", "9780441172719")));
    }

    @Test
    void extractIsbnFallsBackToFirstAvailable() {
        assertEquals("0441172717", BookMetadataService.extractIsbn(List.of("0441172717")));
    }

    @Test
    void buildCoverUrlReturnsNullWithoutCoverId() {
        assertNull(BookMetadataService.buildCoverUrl(null));
    }

    @Test
    void searchExternalReturnsEmptyWhenDocsMissing() {
        when(openLibraryClient.search(eq(EMPTY), eq(10), eq(1)))
                .thenReturn(new OpenLibrarySearchResponse(null, 0));

        assertEquals(0, bookMetadataService.searchExternal(EMPTY, 0, 10).size());
    }

    @Test
    void applyOpenLibraryDocPopulatesBookFields() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                NEUROMANCER,
                List.of(WILLIAM_GIBSON),
                List.of("9780441569595"),
                12345L,
                271,
                "/works/OL123W"
        );
        Book book = new Book();

        bookMetadataService.applyOpenLibraryDoc(book, doc);

        assertEquals(NEUROMANCER, book.getTitle());
        assertEquals(List.of(WILLIAM_GIBSON), book.getAuthors());
        assertEquals(BookSource.OPEN_LIBRARY, book.getSource());
        assertEquals("9780441569595", book.getIsbn());
    }

    @Test
    void extractIsbnReturnsNullForNullList() {
        assertNull(BookMetadataService.extractIsbn(null));
    }

    @TestConfiguration
    @Import({CacheConfig.class, BookMetadataService.class})
    static class CacheTestConfig {

        @Bean
        OpenLibraryClient openLibraryClient() {
            return org.mockito.Mockito.mock(OpenLibraryClient.class);
        }

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        DependencyMetrics dependencyMetrics(SimpleMeterRegistry meterRegistry) {
            return new DependencyMetrics(meterRegistry);
        }
    }

    @Nested
    @SpringJUnitConfig(BookMetadataServiceTest.CacheTestConfig.class)
    class SearchExternalCache {

        @Autowired
        private BookMetadataService bookMetadataService;

        @Autowired
        private OpenLibraryClient openLibraryClient;

        @Test
        void cachesResultsByQueryPageAndSize() {
            OpenLibraryDoc doc = new OpenLibraryDoc(
                    DUNE,
                    List.of(FRANK_HERBERT),
                    List.of("9780441172719"),
                    9266751L,
                    688,
                    "/works/OL893479W"
            );
            when(openLibraryClient.search(eq(DUNE_2), eq(10), eq(1)))
                    .thenReturn(new OpenLibrarySearchResponse(List.of(doc), 1));

            assertEquals(1, bookMetadataService.searchExternal(DUNE_2, 0, 10).size());
            assertEquals(1, bookMetadataService.searchExternal(DUNE_2, 0, 10).size());

            verify(openLibraryClient, times(1)).search(eq(DUNE_2), eq(10), eq(1));
        }
    }
}
