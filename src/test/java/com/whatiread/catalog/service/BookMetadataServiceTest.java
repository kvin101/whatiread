package com.whatiread.catalog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.catalog.api.BookPreviewDto;
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
import java.util.Map;
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
    private static final String WORK_KEY = "/works/OL82537W";
    @Mock
    private OpenLibraryClient openLibraryClient;

    @InjectMocks
    private BookMetadataService bookMetadataService;

    @Test
    void searchExternalMapsOpenLibraryResults() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                DUNE,
                null,
                List.of(FRANK_HERBERT),
                List.of("9780441172719", "0441172717"),
                9266751L,
                null,
                688,
                1965,
                "/works/OL893479W"
        );
        when(openLibraryClient.search(eq(DUNE_2), eq(10), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(doc), 1));

        List<BookSearchResultDto> results = bookMetadataService.searchExternal(DUNE_2, 0, 10);

        assertEquals(1, results.size());
        BookSearchResultDto result = results.getFirst();
        assertEquals(DUNE, result.title());
        assertEquals(List.of(FRANK_HERBERT), result.authors());
        assertEquals("9780441172719", result.isbn());
        assertEquals(688, result.pageCount());
        assertEquals(1965, result.publishYear());
        assertEquals("https://covers.openlibrary.org/b/id/9266751-M.jpg", result.coverUrl());
    }

    @Test
    void applyOpenLibraryDocDoesNotReplaceExistingAuthors() {
        Book book = new Book();
        book.setAuthors(List.of("Existing Author"));
        OpenLibraryDoc doc = new OpenLibraryDoc(
                NEUROMANCER,
                null,
                List.of(WILLIAM_GIBSON),
                null,
                null,
                null,
                271,
                1984,
                WORK_KEY
        );

        bookMetadataService.applyOpenLibraryDoc(book, doc);

        assertEquals(List.of("Existing Author"), book.getAuthors());
    }

    @Test
    void getExternalPreviewThrowsForUnsupportedExternalId() {
        assertThrows(IllegalArgumentException.class, () -> bookMetadataService.getExternalPreview("invalid-id"));
    }

    @Test
    void getExternalPreviewReturnsNullWhenWorkPayloadMissing() {
        when(openLibraryClient.getWork("OL123W")).thenReturn(Map.of());

        assertNull(bookMetadataService.getExternalPreview("/works/OL123W"));
    }

    @Test
    void getExternalPreviewNormalizesExternalIdWithoutLeadingSlash() {
        when(openLibraryClient.getWork("OL123W"))
                .thenReturn(Map.of("title", NEUROMANCER, "first_publish_date", "1984"));

        BookPreviewDto preview = bookMetadataService.getExternalPreview("works/OL123W");

        assertEquals(NEUROMANCER, preview.title());
        assertEquals(1984, preview.publishYear());
    }

    @Test
    void searchExternalUsesEmptyAuthorsWhenAuthorNameMissing() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                DUNE,
                null,
                null,
                null,
                null,
                null,
                688,
                1965,
                WORK_KEY
        );
        when(openLibraryClient.search(eq(DUNE_2), eq(10), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(doc), 1));

        List<BookSearchResultDto> results = bookMetadataService.searchExternal(DUNE_2, 0, 10);

        assertEquals(1, results.size());
        assertEquals(List.of(), results.getFirst().authors());
    }

    @Test
    void extractIsbnReturnsNullForEmptyList() {
        assertNull(BookMetadataService.extractIsbn(List.of()));
    }

    @Test
    void applyOpenLibraryDocDoesNotOverwriteExistingMetadata() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                "New title",
                "New subtitle",
                List.of("Other Author"),
                List.of("9780000000000"),
                1L,
                "OL999M",
                100,
                2020,
                WORK_KEY
        );
        Book book = new Book();
        book.setTitle("Keep title");
        book.setPageCount(340);
        book.setPublishYear(1998);
        book.setIsbn("9780439064866");
        book.setDescription("Keep description");

        bookMetadataService.applyOpenLibraryDoc(book, doc);

        assertEquals("Keep title", book.getTitle());
        assertEquals(340, book.getPageCount());
        assertEquals(1998, book.getPublishYear());
        assertEquals("9780439064866", book.getIsbn());
        assertEquals("Keep description", book.getDescription());
        assertEquals(BookSource.OPEN_LIBRARY, book.getSource());
        assertEquals(WORK_KEY, book.getExternalId());
    }

    @Test
    void enrichBookMetadataMergesSearchWorkAndEditionDetails() {
        Book book = new Book();
        book.setSource(BookSource.OPEN_LIBRARY);
        book.setExternalId(WORK_KEY);

        OpenLibraryDoc searchDoc = new OpenLibraryDoc(
                "Harry Potter and the Chamber of Secrets",
                null,
                List.of("J. K. Rowling"),
                List.of("9780439064866"),
                15158664L,
                "OL59041259M",
                339,
                1998,
                WORK_KEY
        );
        when(openLibraryClient.search(eq("key:" + WORK_KEY), eq(1), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(searchDoc), 1));
        when(openLibraryClient.getWork("OL82537W"))
                .thenReturn(Map.of(
                        "description", Map.of("value", "A wizard school story."),
                        "first_publish_date", "1998",
                        "subtitle", "Book 2"
                ));
        when(openLibraryClient.getEdition("OL59041259M"))
                .thenReturn(Map.of(
                        "number_of_pages", 258,
                        "isbn_13", List.of("9780439064866"),
                        "covers", List.of(15158664)
                ));

        assertTrue(bookMetadataService.enrichBookMetadata(book));

        assertEquals(258, book.getPageCount());
        assertEquals(1998, book.getPublishYear());
        assertEquals("A wizard school story.", book.getDescription());
        assertEquals("9780439064866", book.getIsbn());
        assertEquals("Book 2", book.getSubtitle());
        assertEquals("https://covers.openlibrary.org/b/id/15158664-M.jpg", book.getCoverUrl());
    }

    @Test
    void enrichBookMetadataReturnsFalseForNonOpenLibraryBooks() {
        Book book = new Book();
        book.setSource(BookSource.MANUAL);
        book.setExternalId(WORK_KEY);

        assertFalse(bookMetadataService.enrichBookMetadata(book));
    }

    @Test
    void enrichBookMetadataToleratesOpenLibraryFailures() {
        Book book = new Book();
        book.setSource(BookSource.OPEN_LIBRARY);
        book.setExternalId(WORK_KEY);
        when(openLibraryClient.search(eq("key:" + WORK_KEY), eq(1), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenThrow(new RuntimeException("Open Library unavailable"));

        assertFalse(bookMetadataService.enrichBookMetadata(book));
    }

    @Test
    void getExternalPreviewMapsEditionDetails() {
        when(openLibraryClient.getEdition("OL59041259M"))
                .thenReturn(Map.of(
                        "title", "Chamber of Secrets",
                        "number_of_pages", 258,
                        "isbn_13", List.of("9780439064866"),
                        "covers", List.of(15158664L),
                        "description", "Edition blurb.",
                        "publish_date", "1999",
                        "subjects", List.of("Fantasy")
                ));

        BookPreviewDto preview = bookMetadataService.getExternalPreview("/books/OL59041259M");

        assertEquals("Chamber of Secrets", preview.title());
        assertEquals(258, preview.pageCount());
        assertEquals("9780439064866", preview.isbn());
        assertEquals("Edition blurb.", preview.description());
        assertEquals(1999, preview.publishYear());
        assertEquals(List.of("Fantasy"), preview.subjects());
    }

    @Test
    void enrichBookMetadataReturnsFalseWithoutExternalId() {
        Book book = new Book();
        book.setSource(BookSource.OPEN_LIBRARY);

        assertFalse(bookMetadataService.enrichBookMetadata(book));
    }

    @Test
    void enrichBookMetadataSkipsEditionLookupWithoutCoverEditionKey() {
        Book book = new Book();
        book.setSource(BookSource.OPEN_LIBRARY);
        book.setExternalId(WORK_KEY);

        OpenLibraryDoc searchDoc = new OpenLibraryDoc(
                "Harry Potter and the Chamber of Secrets",
                null,
                List.of("J. K. Rowling"),
                null,
                null,
                null,
                339,
                1998,
                WORK_KEY
        );
        when(openLibraryClient.search(eq("key:" + WORK_KEY), eq(1), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(searchDoc), 1));
        when(openLibraryClient.getWork("OL82537W"))
                .thenReturn(Map.of("description", "Work metadata only."));

        bookMetadataService.enrichBookMetadata(book);

        verify(openLibraryClient, never()).getEdition(org.mockito.ArgumentMatchers.anyString());
        assertEquals("Work metadata only.", book.getDescription());
    }

    @Test
    void enrichBookMetadataFallsBackToWorkWhenSearchHasNoDocs() {
        Book book = new Book();
        book.setSource(BookSource.OPEN_LIBRARY);
        book.setExternalId(WORK_KEY);

        when(openLibraryClient.search(eq("key:" + WORK_KEY), eq(1), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(), 0));
        when(openLibraryClient.getWork("OL82537W"))
                .thenReturn(Map.of(
                        "description", "From work metadata only.",
                        "first_publish_date", "1998"
                ));

        assertTrue(bookMetadataService.enrichBookMetadata(book));
        assertEquals("From work metadata only.", book.getDescription());
        assertEquals(1998, book.getPublishYear());
    }

    @Test
    void enrichBookMetadataSkipsWorkLookupForEditionExternalIds() {
        Book book = new Book();
        book.setSource(BookSource.OPEN_LIBRARY);
        book.setExternalId("/books/OL59041259M");

        when(openLibraryClient.search(eq("key:/books/OL59041259M"), eq(1), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(List.of(), 0));

        bookMetadataService.enrichBookMetadata(book);

        verify(openLibraryClient, never()).getWork(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getExternalPreviewEditionUsesIsbn10WhenIsbn13Missing() {
        when(openLibraryClient.getEdition("OL59041259M"))
                .thenReturn(Map.of(
                        "title", "Chamber of Secrets",
                        "isbn_10", List.of("0439064864")
                ));

        BookPreviewDto preview = bookMetadataService.getExternalPreview("/books/OL59041259M");

        assertEquals("0439064864", preview.isbn());
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
        when(openLibraryClient.search(eq(EMPTY), eq(10), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                .thenReturn(new OpenLibrarySearchResponse(null, 0));

        assertEquals(0, bookMetadataService.searchExternal(EMPTY, 0, 10).size());
    }

    @Test
    void applyOpenLibraryDocPopulatesBookFields() {
        OpenLibraryDoc doc = new OpenLibraryDoc(
                NEUROMANCER,
                null,
                List.of(WILLIAM_GIBSON),
                List.of("9780441569595"),
                12345L,
                null,
                271,
                null,
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
    void getExternalPreviewMapsWorkDetails() {
        when(openLibraryClient.getWork("OL893479W"))
                .thenReturn(Map.of(
                        "title", DUNE,
                        "description", Map.of("value", "Epic desert planet saga."),
                        "first_publish_date", "1965",
                        "subjects", List.of("Science fiction", "Fantasy")
                ));

        BookPreviewDto preview = bookMetadataService.getExternalPreview("/works/OL893479W");

        assertEquals(DUNE, preview.title());
        assertEquals("Epic desert planet saga.", preview.description());
        assertEquals(1965, preview.publishYear());
        assertEquals(List.of("Science fiction", "Fantasy"), preview.subjects());
        assertEquals(BookSource.OPEN_LIBRARY, preview.source());
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
                    null,
                    List.of(FRANK_HERBERT),
                    List.of("9780441172719"),
                    9266751L,
                    null,
                    688,
                    1965,
                    "/works/OL893479W"
            );
            when(openLibraryClient.search(eq(DUNE_2), eq(10), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS)))
                    .thenReturn(new OpenLibrarySearchResponse(List.of(doc), 1));

            assertEquals(1, bookMetadataService.searchExternal(DUNE_2, 0, 10).size());
            assertEquals(1, bookMetadataService.searchExternal(DUNE_2, 0, 10).size());

            verify(openLibraryClient, times(1)).search(eq(DUNE_2), eq(10), eq(1), eq(OpenLibraryClient.SEARCH_FIELDS));
        }
    }
}
