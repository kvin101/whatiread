package com.whatiread.shared.web;

/**
 * Application-specific HTTP header names not covered by {@link org.springframework.http.HttpHeaders}.
 */
public final class AppHttpHeaders {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String RETRY_AFTER = "Retry-After";

    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String REFERRER_POLICY = "Referrer-Policy";
    public static final String PERMISSIONS_POLICY = "Permissions-Policy";
    public static final String X_XSS_PROTECTION = "X-XSS-Protection";
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    private AppHttpHeaders() {
    }
}
