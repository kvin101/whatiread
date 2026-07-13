package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class TracingSamplingConfigTest {

    @Autowired
    private Sampler sampler;

    @Autowired
    private TracingProperties tracingProperties;

    @Autowired
    private Environment environment;

    @Test
    void usesParentBasedTraceIdRatioSamplerWithDevDefault() {
        assertThat(sampler.getDescription()).contains("ParentBased");
        assertThat(sampler.getDescription()).contains("TraceIdRatioBased");
        assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("1.0");
        assertThat(tracingProperties.getSampling().getProbability()).isEqualTo(1.0f);
    }
}
