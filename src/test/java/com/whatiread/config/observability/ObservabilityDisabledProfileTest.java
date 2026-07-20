package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest(properties = {
        "whatiread.security.jwt.secret=test-secret-key-minimum-thirty-two-characters-long",
        "whatiread.security.jwt.access-ttl-minutes=15",
        "whatiread.security.jwt.refresh-ttl-days=7",
        "whatiread.rate-limit.enabled=false"
})
@ActiveProfiles("no-observability")
class ObservabilityDisabledProfileTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void noObservabilityProfileDisablesExportAndPrometheus() {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info");
        assertThat(environment.getProperty("jdbc.opentelemetry.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.openfeign.micrometer.enabled", Boolean.class))
                .isFalse();
        assertThat(environment.acceptsProfiles("no-observability")).isTrue();
    }
}

class ObservabilityProfileEnvironmentPostProcessorTest {

    @Test
    void addsNoObservabilityProfileWhenEnvVarFalse() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of(ObservabilityProfileEnvironmentPostProcessor.OBSERVABILITY_ENABLED, "false")));

        new ObservabilityProfileEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new org.springframework.boot.SpringApplication());

        assertThat(environment.getActiveProfiles())
                .contains(ObservabilityProfileEnvironmentPostProcessor.NO_OBSERVABILITY_PROFILE);
    }

    @Test
    void leavesProfilesUnchangedWhenEnvVarTrue() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of(ObservabilityProfileEnvironmentPostProcessor.OBSERVABILITY_ENABLED, "true")));

        new ObservabilityProfileEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new org.springframework.boot.SpringApplication());

        assertThat(environment.getActiveProfiles())
                .doesNotContain(ObservabilityProfileEnvironmentPostProcessor.NO_OBSERVABILITY_PROFILE);
    }

    @Test
    void addsNoObservabilityProfileWhenDevProfileActive() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of(
                        ObservabilityProfileEnvironmentPostProcessor.OBSERVABILITY_ENABLED, "true",
                        ObservabilityProfileEnvironmentPostProcessor.SPRING_PROFILES_ACTIVE, "dev")));

        new ObservabilityProfileEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new org.springframework.boot.SpringApplication());

        assertThat(environment.getActiveProfiles())
                .contains(ObservabilityProfileEnvironmentPostProcessor.NO_OBSERVABILITY_PROFILE);
    }
}
