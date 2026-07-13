package com.whatiread.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class StringListJsonConverterTest {

    private static final String EMPTY_JSON_ARRAY = "[]";

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void serializesNullAndEmptyAsEmptyArray() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo(EMPTY_JSON_ARRAY);
        assertThat(converter.convertToDatabaseColumn(List.of())).isEqualTo(EMPTY_JSON_ARRAY);
    }

    @Test
    void roundTripsAuthorList() {
        List<String> authors = List.of("Ada Lovelace", "Alan Turing");
        String json = converter.convertToDatabaseColumn(authors);

        assertThat(converter.convertToEntityAttribute(json)).isEqualTo(authors);
    }

    @Test
    void deserializesBlankAsEmptyList() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to deserialize");
    }
}
