package com.whatiread.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsernameUtilsTest {

    @Test
    void normalizeTrimsAndLowercases() {
        assertThat(UsernameUtils.normalize("  Alice_01  ")).isEqualTo("alice_01");
    }

    @Test
    void validateAcceptsValidUsername() {
        UsernameUtils.validate("reader_42");
    }

    @Test
    void validateRejectsTooShort() {
        assertThatThrownBy(() -> UsernameUtils.validate("ab"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3");
    }

    @Test
    void validateRejectsInvalidCharacters() {
        assertThatThrownBy(() -> UsernameUtils.validate("1bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("letter");
    }

    @Test
    void validateRejectsReservedWords() {
        assertThatThrownBy(() -> UsernameUtils.validate("admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void backfillFromUserIdProducesStableHandle() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertThat(UsernameUtils.backfillFromUserId(id)).isEqualTo("u550e8400e29b41d4a716446655440");
    }
}
