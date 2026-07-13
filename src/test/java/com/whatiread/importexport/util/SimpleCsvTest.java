package com.whatiread.importexport.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SimpleCsvTest {


    private static final String TITLE = "Title";
    private static final String DUNE = "Dune";

    @Test
    void parseReadsQuotedFieldsWithCommas() {
        List<Map<String, String>> rows = SimpleCsv.parse("""
                Title,Author
                "Dune, Part 1",Frank Herbert
                """);

        assertThat(rows).hasSize(1);
        assertThat(SimpleCsv.get(rows.getFirst(), TITLE)).isEqualTo("Dune, Part 1");
    }

    @Test
    void parseSkipsBlankRows() {
        List<Map<String, String>> rows = SimpleCsv.parse("""
                Title,Author
                Dune,Frank Herbert
                
                """);

        assertThat(rows).hasSize(1);
    }

    @Test
    void getMatchesCaseInsensitiveHeader() {
        Map<String, String> row = Map.of("title", DUNE);

        assertThat(SimpleCsv.get(row, TITLE)).isEqualTo(DUNE);
    }

    @Test
    void getReturnsEmptyWhenAllNamesMissing() {
        assertThat(SimpleCsv.get(Map.of("author", "Frank Herbert"), TITLE, "Name")).isEmpty();
    }

    @Test
    void parseHandlesEscapedQuotes() {
        List<Map<String, String>> rows = SimpleCsv.parse("Title,Author\n\"Say \"\"Hello\"\"\",Ada\n");

        assertThat(rows).hasSize(1);
        assertThat(SimpleCsv.get(rows.getFirst(), TITLE)).isEqualTo("Say \"Hello\"");
    }

    @Test
    void parseReturnsEmptyForBlankContent() {
        assertThat(SimpleCsv.parse("   ")).isEmpty();
    }

    @Test
    void parsePadsMissingTrailingColumns() {
        List<Map<String, String>> rows = SimpleCsv.parse("""
                Title,Author,Rating
                Dune
                """);

        assertThat(SimpleCsv.get(rows.getFirst(), "Author")).isEmpty();
        assertThat(SimpleCsv.get(rows.getFirst(), TITLE)).isEqualTo(DUNE);
    }
}
