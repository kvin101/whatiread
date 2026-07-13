package com.whatiread.shared.web;

import java.net.URI;

/**
 * RFC 7807 problem type URIs and media type used across API error responses.
 */
public final class ProblemTypes {

    public static final String BASE_URI = "https://whatiread.dev/problems";
    public static final String MEDIA_TYPE = "application/problem+json";

    public static final String NOT_FOUND = "not-found";
    public static final String CONFLICT = "conflict";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String FORBIDDEN = "forbidden";
    public static final String INTERNAL_ERROR = "internal-error";
    public static final String BAD_REQUEST = "bad-request";
    public static final String VALIDATION = "validation";
    public static final String RATE_LIMIT = "rate-limit";

    private ProblemTypes() {
    }

    public static URI uri(String type) {
        return URI.create(uriString(type));
    }

    public static String uriString(String type) {
        return BASE_URI + "/" + type;
    }
}
