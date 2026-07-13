package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class DependencyMetricsTest {

    private static final String BOOKS_CACHE = "books";
    private static final String CACHE_GET_OPERATION = "get";
    private static final String CACHE_EVICT_OPERATION = "evict";
    private static final String CACHE_CLEAR_OPERATION = "clear";
    private static final String OPEN_LIBRARY_SERVICE = "open-library";
    private static final String SEARCH_OPERATION = "search";
    private static final String USER_BOOK_TABLE = "user_book";
    private static final String SELECT_OPERATION = "select";
    private static final String CACHE_TAG = "cache";
    private static final String OPERATION_TAG = "operation";
    private static final String TABLE_TAG = "table";
    private static final String ERROR_TAG = "error";
    private static final String SERVICE_TAG = "service";
    private static final String CONNECTION_ERROR = "connection";
    private static final String TIMEOUT_ERROR = "timeout";
    private static final String UNKNOWN_ERROR = "unknown";
    private static final String OK_RESULT = "ok";
    private static final String STORED_VALUE = "value";
    private static final String CACHED_VALUE = "cached-value";
    private static final String DIRECT_VALUE = "direct-value";
    private static final String CONNECTION_RESET_MESSAGE = "connection reset";
    private static final String TIMED_OUT_MESSAGE = "request timed out";
    private static final String TIMED_OUT_FRAGMENT = "timed out";
    private static final String BOOM_MESSAGE = "boom";
    private static final String ILLEGAL_STATE_EXCEPTION = "IllegalStateException";
    private static final String RUNTIME_EXCEPTION = "RuntimeException";

    private SimpleMeterRegistry meterRegistry;
    private DependencyMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new DependencyMetrics(meterRegistry);
    }

    @Test
    void recordsDatabaseQueryDurationAndRethrowsErrors() {
        assertThat(metrics.recordDatabaseQuery(SELECT_OPERATION, USER_BOOK_TABLE, () -> OK_RESULT)).isEqualTo(OK_RESULT);
        assertThat(meterRegistry.get(MetricNames.DB_QUERY_DURATION).timer().count()).isEqualTo(1);

        assertThatThrownBy(() -> metrics.recordDatabaseQuery(
                SELECT_OPERATION, USER_BOOK_TABLE, () -> {
                    throw new RuntimeException(CONNECTION_RESET_MESSAGE);
                })).isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.get(MetricNames.DB_ERRORS)
                .tag(OPERATION_TAG, SELECT_OPERATION)
                .tag(TABLE_TAG, USER_BOOK_TABLE)
                .tag(ERROR_TAG, CONNECTION_ERROR)
                .counter()
                .count()).isEqualTo(1);
    }

    @Test
    void infersCacheHitFromValueWrapper() {
        Cache.ValueWrapper hit = () -> CACHED_VALUE;
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_GET_OPERATION, () -> hit);
        assertThat(meterRegistry.get(MetricNames.CACHE_HITS).tag(CACHE_TAG, BOOKS_CACHE).counter().count()).isEqualTo(1);

        Cache.ValueWrapper miss = () -> null;
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_GET_OPERATION, () -> miss);
        assertThat(meterRegistry.get(MetricNames.CACHE_MISSES).tag(CACHE_TAG, BOOKS_CACHE).counter().count()).isEqualTo(1);
    }

    @Test
    void treatsEvictAndClearAsHits() {
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_EVICT_OPERATION, () -> null);
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_CLEAR_OPERATION, () -> null);

        assertThat(meterRegistry.find(MetricNames.CACHE_HITS).counters()).hasSize(2);
        assertThat(meterRegistry.find(MetricNames.CACHE_HITS).counters().stream().mapToDouble(c -> c.count()).sum())
                .isEqualTo(2);
        assertThat(meterRegistry.find(MetricNames.CACHE_MISSES).counters()).isEmpty();
    }

    @Test
    void honorsExplicitCacheHitFlag() {
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_GET_OPERATION, true, () -> null);
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_GET_OPERATION, false, () -> STORED_VALUE);

        assertThat(meterRegistry.get(MetricNames.CACHE_HITS).tag(CACHE_TAG, BOOKS_CACHE).counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get(MetricNames.CACHE_MISSES).tag(CACHE_TAG, BOOKS_CACHE).counter().count()).isEqualTo(1);
    }

    @Test
    void recordsCacheAndExternalApiErrors() {
        assertThatThrownBy(() -> metrics.recordCacheRequest(
                BOOKS_CACHE, CACHE_GET_OPERATION, () -> {
                    throw new RuntimeException(TIMED_OUT_MESSAGE);
                })).hasMessageContaining(TIMED_OUT_FRAGMENT);

        assertThat(meterRegistry.get(MetricNames.CACHE_ERRORS)
                .tag(ERROR_TAG, TIMEOUT_ERROR)
                .counter()
                .count()).isEqualTo(1);

        assertThatThrownBy(() -> metrics.recordExternalApiCall(
                OPEN_LIBRARY_SERVICE, SEARCH_OPERATION, () -> {
                    throw new IllegalStateException(BOOM_MESSAGE);
                })).isInstanceOf(IllegalStateException.class);

        assertThat(meterRegistry.get(MetricNames.EXTERNAL_API_ERRORS)
                .tag(SERVICE_TAG, OPEN_LIBRARY_SERVICE)
                .tag(ERROR_TAG, ILLEGAL_STATE_EXCEPTION)
                .counter()
                .count()).isEqualTo(1);
    }

    @Test
    void classifiesErrorsByMessageAndType() {
        assertThat(DependencyMetrics.classifyError(null)).isEqualTo(UNKNOWN_ERROR);
        assertThat(DependencyMetrics.classifyError(new RuntimeException("socket connection refused")))
                .isEqualTo(CONNECTION_ERROR);
        assertThat(DependencyMetrics.classifyError(new RuntimeException("upstream timed out")))
                .isEqualTo(TIMEOUT_ERROR);
        assertThat(DependencyMetrics.classifyError(new IllegalArgumentException("bad input")))
                .isEqualTo("IllegalArgumentException");
        assertThat(DependencyMetrics.classifyError(new RuntimeException((String) null)))
                .isEqualTo(RUNTIME_EXCEPTION);
    }

    @Test
    void recordsSuccessfulExternalApiCall() {
        assertThat(metrics.recordExternalApiCall(OPEN_LIBRARY_SERVICE, SEARCH_OPERATION, () -> OK_RESULT)).isEqualTo(OK_RESULT);
        assertThat(meterRegistry.get(MetricNames.EXTERNAL_API_DURATION)
                .tag(SERVICE_TAG, OPEN_LIBRARY_SERVICE)
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void treatsNonWrapperCacheResultAsHit() {
        metrics.recordCacheRequest(BOOKS_CACHE, CACHE_GET_OPERATION, () -> DIRECT_VALUE);
        assertThat(meterRegistry.get(MetricNames.CACHE_HITS).tag(CACHE_TAG, BOOKS_CACHE).counter().count()).isEqualTo(1);
    }
}
