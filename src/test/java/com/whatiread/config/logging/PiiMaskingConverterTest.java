package com.whatiread.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PiiMaskingConverterTest {

    private PiiMaskingConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PiiMaskingConverter();
    }

    @Test
    void returnsNullWhenMessageIsNull() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(null);

        assertThat(converter.convert(event)).isNull();
    }

    @Test
    void masksBearerTokensEmailsAndPasswords() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9 user@secret.com {\"password\":\"s3cret\"}"
        );

        String masked = converter.convert(event);

        assertThat(masked).contains("Bearer ***");
        assertThat(masked).contains("***@secret.com");
        assertThat(masked).contains("\"password\":\"***\"");
        assertThat(masked).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(masked).doesNotContain("user@secret.com");
        assertThat(masked).doesNotContain("s3cret");
    }
}
