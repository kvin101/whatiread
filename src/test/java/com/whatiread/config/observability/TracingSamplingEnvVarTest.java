package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "TRACING_SAMPLE_RATE=0.25",
        "management.tracing.sampling.probability=${TRACING_SAMPLE_RATE:1.0}",
        "whatiread.security.jwt.secret=test-secret-key-minimum-thirty-two-characters-long",
        "whatiread.security.jwt.access-ttl-minutes=15",
        "whatiread.security.jwt.refresh-ttl-days=7",
        "whatiread.rate-limit.enabled=false"
})
class TracingSamplingEnvVarTest {

    @Autowired
    private TracingProperties tracingProperties;

    @Test
    void tracingSampleRateEnvVarBindsToProbability() {
        assertThat(tracingProperties.getSampling().getProbability()).isEqualTo(0.25f);
    }
}
