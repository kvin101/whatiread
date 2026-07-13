package com.whatiread.config.ratelimit;

public record RateLimitDecision(boolean granted, long retryAfterSeconds) {

    public static RateLimitDecision grant() {
        return new RateLimitDecision(true, 0);
    }

    public static RateLimitDecision deny(long retryAfterSeconds) {
        return new RateLimitDecision(false, Math.max(1, retryAfterSeconds));
    }
}
