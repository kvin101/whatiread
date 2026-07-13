package com.whatiread.config;

import com.whatiread.identity.security.AuthenticatedUser;
import com.whatiread.shared.util.HttpRequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("com.whatiread.http.access");

    private final WhatIReadProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestLoggingFilter(WhatIReadProperties properties) {
        this.properties = properties;
    }

    private static String resolveUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getId().toString();
        }
        return "-";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        WhatIReadProperties.RequestLogging config = properties.requestLogging();
        if (!config.enabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return config.excludePaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            logRequest(request, response, startedAt);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        String method = request.getMethod();
        String path = buildPath(request);
        int status = response.getStatus();
        String client = HttpRequestUtils.clientIp(request);
        String user = resolveUserId();

        ACCESS_LOG.info("{} {} {} {}ms client={} user={}", method, path, status, durationMs, client, user);
    }

    private String buildPath(HttpServletRequest request) {
        WhatIReadProperties.RequestLogging config = properties.requestLogging();
        String path = request.getRequestURI();
        if (config.includeQueryString()) {
            String query = request.getQueryString();
            if (StringUtils.hasText(query)) {
                return path + "?" + query;
            }
        }
        return path;
    }
}
