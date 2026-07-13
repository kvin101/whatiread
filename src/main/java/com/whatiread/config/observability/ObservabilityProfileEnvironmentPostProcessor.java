package com.whatiread.config.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Activates the {@code no-observability} Spring profile when {@code OBSERVABILITY_ENABLED=false}, before config
 * data is loaded so profile-specific settings apply in Docker and tests.
 */
public class ObservabilityProfileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String OBSERVABILITY_ENABLED = "OBSERVABILITY_ENABLED";
    static final String NO_OBSERVABILITY_PROFILE = "no-observability";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Boolean.parseBoolean(environment.getProperty(OBSERVABILITY_ENABLED, "true"))) {
            environment.addActiveProfile(NO_OBSERVABILITY_PROFILE);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
