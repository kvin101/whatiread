package com.whatiread.config.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Per-client-IP Resilience4j rate limiters. Each tier + IP pair gets its own {@link RateLimiter} instance (cached with eviction for single-instance
 * deployments).
 */
@Component
public class ClientRateLimiter {

    private final RateLimiterRegistry registry;
    private final Cache<String, RateLimiter> limiters;

    public ClientRateLimiter(RateLimiterRegistry registry) {
        this.registry = registry;
        this.limiters = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(5))
                .maximumSize(50_000)
                .removalListener((String name, RateLimiter limiter, com.github.benmanes.caffeine.cache.RemovalCause cause) ->
                        this.registry.remove(name))
                .build();
    }

    private static long retryAfterSeconds(RateLimiter limiter) {
        Duration refresh = limiter.getRateLimiterConfig().getLimitRefreshPeriod();
        long refreshSeconds = Math.max(1, refresh.getSeconds());
        long now = Instant.now().getEpochSecond();
        long remaining = refreshSeconds - (now % refreshSeconds);
        return remaining > 0 ? remaining : refreshSeconds;
    }

    public RateLimitDecision tryAcquire(RateLimitTier tier, String clientIp) {
        String name = tier.configName() + ":" + clientIp;
        RateLimiter limiter = limiters.get(name, key -> registry.rateLimiter(key, tier.configName()));
        if (limiter.acquirePermission()) {
            return RateLimitDecision.grant();
        }
        return RateLimitDecision.deny(retryAfterSeconds(limiter));
    }
}
