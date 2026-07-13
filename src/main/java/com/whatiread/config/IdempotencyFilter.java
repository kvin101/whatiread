package com.whatiread.config;

import com.whatiread.shared.idempotency.IdempotencyRecord;
import com.whatiread.shared.idempotency.IdempotencyService;
import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.AppHttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING = Set.of("POST", "PATCH", "PUT");

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!MUTATING.contains(request.getMethod())) {
            return true;
        }
        String key = request.getHeader(AppHttpHeaders.IDEMPOTENCY_KEY);
        return key == null || key.isBlank() || !request.getRequestURI().startsWith(ApiPaths.V1_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = request.getHeader(AppHttpHeaders.IDEMPOTENCY_KEY).trim();
        Optional<IdempotencyRecord> existing = idempotencyService.findValid(key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            response.setStatus(record.getResponseStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            if (record.getResponseBody() != null) {
                response.getOutputStream().write(record.getResponseBody().getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapped);
        if (wrapped.getStatus() < 500) {
            String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
            idempotencyService.store(
                    key,
                    request.getMethod(),
                    request.getRequestURI(),
                    wrapped.getStatus(),
                    body.isBlank() ? null : body
            );
        }
        wrapped.copyBodyToResponse();
    }
}
