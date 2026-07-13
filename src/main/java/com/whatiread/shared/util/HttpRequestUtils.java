package com.whatiread.shared.util;

import jakarta.servlet.http.HttpServletRequest;

public final class HttpRequestUtils {

    private HttpRequestUtils() {
    }

    /**
     * Client IP after servlet forwarded-header processing ({@code server.forward-headers-strategy}). Do not read {@code X-Forwarded-For} directly —
     * it is spoofable when the API is reachable without a trusted reverse proxy (see {@code server.tomcat.remoteip.internal-proxies} in prod).
     */
    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
