package com.whatiread.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.AppHttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

    private static final String X_CONTENT_TYPE_OPTIONS_NOSNIFF = "nosniff";
    private static final String X_FRAME_OPTIONS_DENY = "DENY";
    private static final String REFERRER_POLICY_STRICT_ORIGIN = "strict-origin-when-cross-origin";
    private static final String PERMISSIONS_POLICY_CAMERA_MIC_GEO = "camera=(), microphone=(), geolocation=()";
    private static final String X_XSS_PROTECTION_DISABLED = "0";
    private static final String CSP_DEFAULT_SRC_SELF = "default-src 'self'";
    private static final String CSP_SCRIPT_SRC_SELF = "script-src 'self'";
    private static final String CSP_STYLE_SRC_SELF_UNSAFE_INLINE = "style-src 'self' 'unsafe-inline'";
    private static final String CSP_IMG_SRC_SELF_DATA_HTTPS = "img-src 'self' data: https:";
    private static final String CSP_FONT_SRC_SELF = "font-src 'self'";
    private static final String CSP_CONNECT_SRC_SELF = "connect-src 'self'";
    private static final String CSP_FRAME_ANCESTORS_NONE = "frame-ancestors 'none'";
    private static final String CSP_BASE_URI_SELF = "base-uri 'self'";
    private static final String CSP_FORM_ACTION_SELF = "form-action 'self'";
    private static final String HSTS_MAX_AGE = "max-age=31536000; includeSubDomains";

    private SecurityHeadersFilter filter;
    private FilterChain filterChain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        filterChain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    @Test
    void setsBaselineSecurityHeaders() throws ServletException, IOException {
        filter.doFilter(new MockHttpServletRequest("GET", ApiPaths.BOOKS), response, filterChain);

        assertThat(response.getHeader(AppHttpHeaders.X_CONTENT_TYPE_OPTIONS)).isEqualTo(X_CONTENT_TYPE_OPTIONS_NOSNIFF);
        assertThat(response.getHeader(AppHttpHeaders.X_FRAME_OPTIONS)).isEqualTo(X_FRAME_OPTIONS_DENY);
        assertThat(response.getHeader(AppHttpHeaders.REFERRER_POLICY)).isEqualTo(REFERRER_POLICY_STRICT_ORIGIN);
        assertThat(response.getHeader(AppHttpHeaders.PERMISSIONS_POLICY))
                .isEqualTo(PERMISSIONS_POLICY_CAMERA_MIC_GEO);
        assertThat(response.getHeader(AppHttpHeaders.X_XSS_PROTECTION)).isEqualTo(X_XSS_PROTECTION_DISABLED);
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void setsRestrictiveContentSecurityPolicy() throws ServletException, IOException {
        filter.doFilter(new MockHttpServletRequest("GET", "/"), response, filterChain);

        String csp = response.getHeader(AppHttpHeaders.CONTENT_SECURITY_POLICY);
        assertThat(csp).contains(CSP_DEFAULT_SRC_SELF);
        assertThat(csp).contains(CSP_SCRIPT_SRC_SELF);
        assertThat(csp).contains(CSP_STYLE_SRC_SELF_UNSAFE_INLINE);
        assertThat(csp).contains(CSP_IMG_SRC_SELF_DATA_HTTPS);
        assertThat(csp).contains(CSP_FONT_SRC_SELF);
        assertThat(csp).contains(CSP_CONNECT_SRC_SELF);
        assertThat(csp).contains(CSP_FRAME_ANCESTORS_NONE);
        assertThat(csp).contains(CSP_BASE_URI_SELF);
        assertThat(csp).contains(CSP_FORM_ACTION_SELF);
    }

    @Test
    void addsHstsOnlyForSecureRequests() throws ServletException, IOException {
        MockHttpServletRequest insecure = new MockHttpServletRequest("GET", "/");
        filter.doFilter(insecure, response, filterChain);
        assertThat(response.getHeader(AppHttpHeaders.STRICT_TRANSPORT_SECURITY)).isNull();

        response = new MockHttpServletResponse();
        MockHttpServletRequest secure = new MockHttpServletRequest("GET", "/");
        secure.setSecure(true);
        filter.doFilter(secure, response, filterChain);
        assertThat(response.getHeader(AppHttpHeaders.STRICT_TRANSPORT_SECURITY))
                .isEqualTo(HSTS_MAX_AGE);
    }
}
