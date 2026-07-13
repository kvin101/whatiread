package com.whatiread.config.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientRateLimiterTest {

    private static final String READ_TIER = "read";
    private static final String TEST_CLIENT_IP = "198.51.100.9";

    private static RateLimiterConfig config(int limitForPeriod) {
        return RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    @Test
    void publishesResilience4jRateLimiterMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RateLimiterRegistry registry = RateLimiterRegistry.of(Map.of(READ_TIER, config(2)));
        TaggedRateLimiterMetrics.ofRateLimiterRegistry(registry).bindTo(meterRegistry);
        ClientRateLimiter clientRateLimiter = new ClientRateLimiter(registry);

        assertThat(clientRateLimiter.tryAcquire(RateLimitTier.READ, TEST_CLIENT_IP).granted()).isTrue();
        assertThat(clientRateLimiter.tryAcquire(RateLimitTier.READ, TEST_CLIENT_IP).granted()).isTrue();
        assertThat(clientRateLimiter.tryAcquire(RateLimitTier.READ, TEST_CLIENT_IP).granted()).isFalse();

        assertThat(meterRegistry.find("resilience4j.ratelimiter.available.permissions").gauges())
                .isNotEmpty();
        assertThat(meterRegistry.find("resilience4j.ratelimiter.waiting_threads").gauges())
                .isNotEmpty();
    }
}
