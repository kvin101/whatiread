package com.whatiread.config.ratelimit;

/**
 * Named Resilience4j rate-limiter tiers. Config names match {@code resilience4j.ratelimiter.configs.*} in {@code application.yaml}.
 */
public enum RateLimitTier {

    STRICT_AUTH("strict-auth"),
    SEARCH("search"),
    WRITE("write"),
    READ("read"),
    DEFAULT("default");

    private final String configName;

    RateLimitTier(String configName) {
        this.configName = configName;
    }

    public String configName() {
        return configName;
    }
}
