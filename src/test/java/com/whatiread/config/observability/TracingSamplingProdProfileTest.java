package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "DATABASE_PASSWORD=prod-test-password-minimum-length",
        "JWT_SECRET=prod-test-secret-minimum-thirty-two-characters"
})
class TracingSamplingProdProfileTest {

    @Autowired
    private TracingProperties tracingProperties;

    @Test
    void prodProfileExportsAllSpansByDefault() {
        assertThat(tracingProperties.getSampling().getProbability()).isEqualTo(1.0f);
    }
}
