package com.whatiread.config.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.WebPaths;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitTierResolverTest {

    private static final String RESOURCE_ID = "/1";
    private static final String LOGIN_PATH = "/login";

    private final RateLimitTierResolver resolver = new RateLimitTierResolver();

    static Stream<Arguments> apiPathTierCases() {
        return Stream.of(
                Arguments.of(ApiPaths.AUTH + LOGIN_PATH, "POST", RateLimitTier.STRICT_AUTH),
                Arguments.of(ApiPaths.SETUP + "/complete", "POST", RateLimitTier.STRICT_AUTH),
                Arguments.of(ApiPaths.IMPORT_GOODREADS, "POST", RateLimitTier.STRICT_IMPORT),
                Arguments.of(ApiPaths.BOOKS_SEARCH, "GET", RateLimitTier.SEARCH),
                Arguments.of(ApiPaths.BOOKS_SUGGEST, "GET", RateLimitTier.SEARCH),
                Arguments.of(ApiPaths.BOOKS_SEARCH + "/extra", "GET", RateLimitTier.SEARCH),
                Arguments.of(ApiPaths.LIBRARY, "POST", RateLimitTier.WRITE),
                Arguments.of(ApiPaths.SHELVES + RESOURCE_ID, "PUT", RateLimitTier.WRITE),
                Arguments.of(ApiPaths.FRIENDS + RESOURCE_ID, "PATCH", RateLimitTier.WRITE),
                Arguments.of(ApiPaths.COMMENTS + RESOURCE_ID, "DELETE", RateLimitTier.WRITE),
                Arguments.of(ApiPaths.BOOKS, "GET", RateLimitTier.READ),
                Arguments.of(ApiPaths.STATUS, "GET", RateLimitTier.READ),
                Arguments.of(ApiPaths.STATUS, "HEAD", RateLimitTier.READ),
                Arguments.of(ApiPaths.BOOKS, "OPTIONS", RateLimitTier.DEFAULT)
        );
    }

    private static HttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    @ParameterizedTest
    @MethodSource("apiPathTierCases")
    void resolvesTierForApiPaths(String path, String method, RateLimitTier expected) {
        assertThat(resolver.resolve(request(method, path))).contains(expected);
    }

    @Test
    void excludesActuatorWebSocketAndStaticPaths() {
        assertThat(resolver.resolve(request("GET", WebPaths.ACTUATOR_HEALTH))).isEmpty();
        assertThat(resolver.resolve(request("GET", WebPaths.ACTUATOR_PROMETHEUS))).isEmpty();
        assertThat(resolver.resolve(request("GET", WebPaths.WS))).isEmpty();
        assertThat(resolver.resolve(request("GET", WebPaths.WS + "/info"))).isEmpty();
        assertThat(resolver.resolve(request("GET", WebPaths.STATIC_PREFIX + "app.js"))).isEmpty();
    }

    @Test
    void excludesNonApiPaths() {
        assertThat(resolver.resolve(request("GET", WebPaths.SWAGGER_UI))).isEmpty();
        assertThat(resolver.resolve(request("GET", WebPaths.OPENAPI_DOCS))).isEmpty();
    }

    @Test
    void authTierTakesPrecedenceOverWrite() {
        assertThat(resolver.resolve(request("POST", ApiPaths.AUTH + LOGIN_PATH))).contains(RateLimitTier.STRICT_AUTH);
    }

    @Test
    void importTierTakesPrecedenceOverWrite() {
        assertThat(resolver.resolve(request("POST", ApiPaths.IMPORT_GOODREADS))).contains(RateLimitTier.STRICT_IMPORT);
    }

    @Test
    void searchTierAppliesOnlyToGetBookSearch() {
        assertThat(resolver.resolve(request("POST", ApiPaths.BOOKS_SEARCH))).contains(RateLimitTier.WRITE);
        assertThat(resolver.resolve(request("POST", ApiPaths.BOOKS_SUGGEST))).contains(RateLimitTier.WRITE);
        assertThat(resolver.resolve(request("GET", ApiPaths.BOOKS + "/1"))).contains(RateLimitTier.READ);
    }
}
