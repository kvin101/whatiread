package com.whatiread.config;

import com.whatiread.config.ratelimit.ClientRateLimiter;
import com.whatiread.config.ratelimit.RateLimitDecision;
import com.whatiread.config.ratelimit.RateLimitTier;
import com.whatiread.config.ratelimit.RateLimitTierResolver;
import com.whatiread.shared.util.HttpRequestUtils;
import com.whatiread.shared.web.AppHttpHeaders;
import com.whatiread.shared.web.ProblemTypes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tiered per-IP rate limiting backed by Resilience4j {@link io.github.resilience4j.ratelimiter.RateLimiter}. Limits are configured in
 * {@code resilience4j.ratelimiter.configs.*}; toggle with {@code whatiread.rate-limit.enabled}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final WhatIReadProperties properties;
    private final RateLimitTierResolver tierResolver;
    private final ClientRateLimiter clientRateLimiter;

    public RateLimitFilter(
            WhatIReadProperties properties,
            RateLimitTierResolver tierResolver,
            ClientRateLimiter clientRateLimiter
    ) {
        this.properties = properties;
        this.tierResolver = tierResolver;
        this.clientRateLimiter = clientRateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        WhatIReadProperties.RateLimit config = properties.rateLimit();
        if (config == null || !config.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitTier tier = tierResolver.resolve(request).orElse(null);
        if (tier == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = HttpRequestUtils.clientIp(request);
        RateLimitDecision decision = clientRateLimiter.tryAcquire(tier, clientIp);
        if (!decision.granted()) {
            response.setStatus(429);
            response.setHeader(AppHttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(ProblemTypes.MEDIA_TYPE);
            response.getWriter().write("""
                    {"type":"%s","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded. Try again shortly."}
                    """.formatted(ProblemTypes.uriString(ProblemTypes.RATE_LIMIT)));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
