package com.whatiread.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.whatiread.config.ratelimit.ClientRateLimiter;
import com.whatiread.config.ratelimit.RateLimitTierResolver;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.AppHttpHeaders;
import com.whatiread.shared.web.ProblemTypes;
import com.whatiread.shared.web.WebPaths;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String OTHER_CLIENT_IP_1 = "203.0.113.1";
    private static final String OTHER_CLIENT_IP_2 = "203.0.113.2";
    private static final String READ_TIER = "read";
    private static final String STRICT_AUTH_TIER = "strict-auth";
    private static final String STRICT_IMPORT_TIER = "strict-import";
    private static final String WRITE_TIER = "write";
    private static final String TOO_MANY_REQUESTS = "Too Many Requests";

    private FilterChain filterChain;
    private MockHttpServletResponse response;

    private static MockHttpServletRequest request(String method, String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static RateLimitFilter filter(boolean enabled, Map<String, RateLimiterConfig> configs) {
        RateLimiterRegistry registry = RateLimiterRegistry.of(configs);
        ClientRateLimiter clientRateLimiter = new ClientRateLimiter(registry);
        WhatIReadProperties properties = new WhatIReadProperties(
                null,
                null,
                null,
                null,
                new WhatIReadProperties.RateLimit(enabled),
                null
        );
        return new RateLimitFilter(properties, new RateLimitTierResolver(), clientRateLimiter);
    }

    private static RateLimiterConfig config(int limitForPeriod) {
        return RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    @BeforeEach
    void setUp() {
        filterChain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    @Test
    void passesThroughWhenRateLimitDisabled() throws ServletException, IOException {
        RateLimitFilter filter = filter(
                false, Map.of(
                        READ_TIER, config(5),
                        STRICT_AUTH_TIER, config(2)
                ));

        invoke(filter, request("GET", ApiPaths.BOOKS, CLIENT_IP));

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsNonApiPaths() throws ServletException, IOException {
        RateLimitFilter filter = filter(true, Map.of(READ_TIER, config(1)));

        invoke(filter, request("GET", WebPaths.ACTUATOR_HEALTH, CLIENT_IP));

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enforcesReadTierPerIp() throws ServletException, IOException {
        RateLimitFilter filter = filter(true, Map.of(READ_TIER, config(3)));

        assertAllowed(filter, request("GET", ApiPaths.BOOKS, CLIENT_IP));
        assertAllowed(filter, request("GET", ApiPaths.LIBRARY, CLIENT_IP));
        assertAllowed(filter, request("GET", ApiPaths.FRIENDS, CLIENT_IP));
        assertRateLimited(filter, request("GET", ApiPaths.SHELVES, CLIENT_IP));
    }

    @Test
    void usesStricterAuthAndImportTiers() throws ServletException, IOException {
        RateLimitFilter filter = filter(
                true, Map.of(
                        READ_TIER, config(100),
                        STRICT_AUTH_TIER, config(2),
                        STRICT_IMPORT_TIER, config(2)
                ));

        assertAllowed(filter, request("POST", ApiPaths.AUTH + "/login", CLIENT_IP));
        assertAllowed(filter, request("POST", ApiPaths.AUTH + "/register", CLIENT_IP));
        assertRateLimited(filter, request("POST", ApiPaths.AUTH + "/refresh", CLIENT_IP));

        assertAllowed(filter, request("POST", ApiPaths.IMPORT_GOODREADS, CLIENT_IP));
        assertAllowed(filter, request("POST", ApiPaths.IMPORT_GOODREADS, CLIENT_IP));
        assertRateLimited(filter, request("POST", ApiPaths.IMPORT_GOODREADS, CLIENT_IP));
    }

    @Test
    void authAndSetupShareStrictAuthTierPerIp() throws ServletException, IOException {
        RateLimitFilter filter = filter(true, Map.of(STRICT_AUTH_TIER, config(2)));

        assertAllowed(filter, request("POST", ApiPaths.AUTH + "/login", CLIENT_IP));
        assertAllowed(filter, request("POST", ApiPaths.SETUP + "/complete", CLIENT_IP));
        assertRateLimited(filter, request("POST", ApiPaths.AUTH + "/refresh", CLIENT_IP));
    }

    @Test
    void isolatesWriteTierFromReadTier() throws ServletException, IOException {
        RateLimitFilter filter = filter(
                true, Map.of(
                        READ_TIER, config(1),
                        WRITE_TIER, config(1)
                ));

        assertAllowed(filter, request("GET", ApiPaths.BOOKS, CLIENT_IP));
        assertRateLimited(filter, request("GET", ApiPaths.LIBRARY, CLIENT_IP));
        assertAllowed(filter, request("POST", ApiPaths.LIBRARY, CLIENT_IP));
    }

    @Test
    void isolatesCountersByClientIp() throws ServletException, IOException {
        RateLimitFilter filter = filter(true, Map.of(READ_TIER, config(1)));

        assertAllowed(filter, request("GET", ApiPaths.BOOKS, OTHER_CLIENT_IP_1));
        assertRateLimited(filter, request("GET", ApiPaths.BOOKS, OTHER_CLIENT_IP_1));
        assertAllowed(filter, request("GET", ApiPaths.BOOKS, OTHER_CLIENT_IP_2));
    }

    @Test
    void returnsProblemJsonWhenRateLimited() throws ServletException, IOException {
        RateLimitFilter filter = filter(true, Map.of(READ_TIER, config(1)));
        FilterChain chain = mock(FilterChain.class);

        invoke(filter, request("GET", ApiPaths.BOOKS, CLIENT_IP), chain);
        invoke(filter, request("GET", ApiPaths.BOOKS, CLIENT_IP), chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo(ProblemTypes.MEDIA_TYPE);
        assertThat(response.getContentAsString()).contains(TOO_MANY_REQUESTS);
        assertThat(response.getContentAsString()).contains(ProblemTypes.RATE_LIMIT);
        assertThat(response.getContentAsString()).contains(ProblemTypes.uriString(ProblemTypes.RATE_LIMIT));
        assertThat(response.getHeader(AppHttpHeaders.RETRY_AFTER)).isNotNull();
        verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private void assertAllowed(RateLimitFilter filter, MockHttpServletRequest request)
            throws ServletException, IOException {
        invoke(filter, request);
        assertThat(response.getStatus()).isEqualTo(200);
        response = new MockHttpServletResponse();
    }

    private void assertRateLimited(RateLimitFilter filter, MockHttpServletRequest request)
            throws ServletException, IOException {
        invoke(filter, request);
        assertThat(response.getStatus()).isEqualTo(429);
        response = new MockHttpServletResponse();
    }

    private void invoke(RateLimitFilter filter, MockHttpServletRequest request)
            throws ServletException, IOException {
        invoke(filter, request, filterChain);
    }

    private void invoke(RateLimitFilter filter, MockHttpServletRequest request, FilterChain chain)
            throws ServletException, IOException {
        filter.doFilter(request, response, chain);
    }
}
