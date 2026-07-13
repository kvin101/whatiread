package com.whatiread.config;

import com.whatiread.shared.web.AppHttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * Mitigates XSS impact when refresh tokens are stored in {@code localStorage} (see README production checklist). HttpOnly cookies would be
     * stronger but require broader auth changes.
     */
    private static final String CONTENT_SECURITY_POLICY = String.join(
            "; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https:",
            "font-src 'self'",
            "connect-src 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "form-action 'self'"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader(AppHttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");
        response.setHeader(AppHttpHeaders.X_FRAME_OPTIONS, "DENY");
        response.setHeader(AppHttpHeaders.REFERRER_POLICY, "strict-origin-when-cross-origin");
        response.setHeader(AppHttpHeaders.PERMISSIONS_POLICY, "camera=(), microphone=(), geolocation=()");
        response.setHeader(AppHttpHeaders.X_XSS_PROTECTION, "0");
        response.setHeader(AppHttpHeaders.CONTENT_SECURITY_POLICY, CONTENT_SECURITY_POLICY);
        if (request.isSecure()) {
            response.setHeader(AppHttpHeaders.STRICT_TRANSPORT_SECURITY, "max-age=31536000; includeSubDomains");
        }
        filterChain.doFilter(request, response);
    }
}
