package com.whatiread.config.observability;

/**
 * Micrometer metric name constants shared across observability components.
 */
public final class MetricNames {

    public static final String HTTP_SERVER_REQUESTS = "http.server.requests";

    public static final String DB_QUERY_DURATION = "db.query.duration";
    public static final String DB_ERRORS = "db.errors";

    public static final String CACHE_HITS = "cache.hits";
    public static final String CACHE_MISSES = "cache.misses";
    public static final String CACHE_ERRORS = "cache.errors";
    public static final String CACHE_REQUEST_DURATION = "cache.request.duration";

    public static final String EXTERNAL_API_DURATION = "external.api.duration";
    public static final String EXTERNAL_API_ERRORS = "external.api.errors";

    private MetricNames() {
    }
}
