package com.whatiread.config.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Activates the {@code no-observability} Spring profile for local/dev Docker and when observability is off,
 * before config data is loaded so profile-specific settings apply in Docker and tests.
 */
public class ObservabilityProfileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String OBSERVABILITY_ENABLED = "OBSERVABILITY_ENABLED";
    static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
    static final String DEV_PROFILE = "dev";
    static final String NO_OBSERVABILITY_PROFILE = "no-observability";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (usesDevProfile(environment) || !Boolean.parseBoolean(environment.getProperty(OBSERVABILITY_ENABLED, "true"))) {
            environment.addActiveProfile(NO_OBSERVABILITY_PROFILE);
        }
    }

    private static boolean usesDevProfile(ConfigurableEnvironment environment) {
        String activeProfiles = environment.getProperty(SPRING_PROFILES_ACTIVE, "");
        for (String profile : activeProfiles.split(",")) {
            if (DEV_PROFILE.equals(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
