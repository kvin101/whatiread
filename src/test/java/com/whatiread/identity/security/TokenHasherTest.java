package com.whatiread.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    private static final String REFRESH_TOKEN = "refresh-token";

    @Test
    void hashesTokensDeterministically() {
        String first = TokenHasher.hash(REFRESH_TOKEN);
        String second = TokenHasher.hash(REFRESH_TOKEN);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
        assertThat(first).matches("[0-9a-f]+");
    }

    @Test
    void producesDifferentHashesForDifferentTokens() {
        assertThat(TokenHasher.hash("token-a")).isNotEqualTo(TokenHasher.hash("token-b"));
    }
}
