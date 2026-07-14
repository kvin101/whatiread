package com.whatiread.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeysetCursorTest {

    @Test
    void roundTripsCursor() {
        Instant updatedAt = Instant.parse("2024-06-01T12:00:00Z");
        UUID id = UUID.randomUUID();

        String encoded = KeysetCursor.encode(updatedAt, id);
        KeysetCursor.Parts parts = KeysetCursor.decode(encoded);

        assertThat(parts.updatedAt()).isEqualTo(updatedAt);
        assertThat(parts.id()).isEqualTo(id);
    }

    @Test
    void decodeBlankReturnsEmptyParts() {
        assertThat(KeysetCursor.decode(null).timestamp()).isNull();
        assertThat(KeysetCursor.decode("").timestamp()).isNull();
        assertThat(KeysetCursor.decode("   ").timestamp()).isNull();
    }
}
