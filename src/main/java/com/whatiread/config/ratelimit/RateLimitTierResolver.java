package com.whatiread.config.ratelimit;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.WebPaths;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps incoming HTTP requests to a rate-limit tier. Excluded paths return empty (no limiting).
 */
@Component
public class RateLimitTierResolver {

    private static boolean isExcluded(String path) {
        return path.startsWith(WebPaths.ACTUATOR)
                || path.startsWith(WebPaths.WS)
                || path.startsWith(WebPaths.STATIC_PREFIX);
    }

    private static boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    public Optional<RateLimitTier> resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (isExcluded(path)) {
            return Optional.empty();
        }
        if (!path.startsWith(ApiPaths.V1_PREFIX)) {
            return Optional.empty();
        }
        String method = request.getMethod();
        if (path.startsWith(ApiPaths.AUTH + "/") || path.startsWith(ApiPaths.SETUP + "/")) {
            if ("GET".equalsIgnoreCase(method) && ApiPaths.AUTH_USERNAME_AVAILABLE.equals(path)) {
                return Optional.of(RateLimitTier.READ);
            }
            return Optional.of(RateLimitTier.STRICT_AUTH);
        }
        if (path.startsWith(ApiPaths.IMPORT + "/")) {
            return Optional.of(RateLimitTier.STRICT_IMPORT);
        }
        if ("GET".equalsIgnoreCase(method)
                && (path.startsWith(ApiPaths.BOOKS_SEARCH)
                || path.startsWith(ApiPaths.BOOKS_SUGGEST)
                || path.startsWith(ApiPaths.USERS_SUGGEST)
                || path.startsWith(ApiPaths.ADMIN_USERS_SUGGEST))) {
            return Optional.of(RateLimitTier.SEARCH);
        }
        if (isWriteMethod(method)) {
            return Optional.of(RateLimitTier.WRITE);
        }
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return Optional.of(RateLimitTier.READ);
        }
        return Optional.of(RateLimitTier.DEFAULT);
    }
}
