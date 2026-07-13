package com.whatiread.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

@Component
public class DependencyMetrics {

    private final MeterRegistry meterRegistry;

    public DependencyMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    static String classifyError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String simpleName = error.getClass().getSimpleName();
        String message = error.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("timeout") || lower.contains("timed out")) {
                return "timeout";
            }
            if (lower.contains("connection")) {
                return "connection";
            }
        }
        return simpleName.isBlank() ? "unknown" : simpleName;
    }

    private static boolean isCacheHit(Object result, String operation) {
        if ("evict".equals(operation) || "clear".equals(operation)) {
            return true;
        }
        if (result == null) {
            return false;
        }
        if (result instanceof Cache.ValueWrapper wrapper) {
            return wrapper.get() != null;
        }
        return true;
    }

    public <T> T recordDatabaseQuery(String operation, String table, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return action.get();
        } catch (RuntimeException ex) {
            recordDatabaseError(operation, table, ex);
            throw ex;
        } finally {
            sample.stop(Timer.builder(MetricNames.DB_QUERY_DURATION)
                    .description("Database query duration")
                    .tag("operation", operation)
                    .tag("table", table)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    public void recordDatabaseError(String operation, String table, Throwable error) {
        meterRegistry.counter(
                        MetricNames.DB_ERRORS,
                        "operation", operation,
                        "table", table,
                        "error", classifyError(error))
                .increment();
    }

    public <T> T recordCacheRequest(String cacheName, String operation, Supplier<T> action) {
        return recordCacheRequest(cacheName, operation, null, action);
    }

    public <T> T recordCacheRequest(String cacheName, String operation, Boolean cacheHit, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = action.get();
            boolean hit = cacheHit != null ? cacheHit : isCacheHit(result, operation);
            if (hit) {
                meterRegistry.counter(
                                MetricNames.CACHE_HITS,
                                "cache", cacheName,
                                "operation", operation)
                        .increment();
            } else {
                meterRegistry.counter(
                                MetricNames.CACHE_MISSES,
                                "cache", cacheName,
                                "operation", operation)
                        .increment();
            }
            return result;
        } catch (RuntimeException ex) {
            meterRegistry.counter(
                            MetricNames.CACHE_ERRORS,
                            "cache", cacheName,
                            "operation", operation,
                            "error", classifyError(ex))
                    .increment();
            throw ex;
        } finally {
            sample.stop(Timer.builder(MetricNames.CACHE_REQUEST_DURATION)
                    .description("Cache request duration")
                    .tag("cache", cacheName)
                    .tag("operation", operation)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    public <T> T recordExternalApiCall(String service, String operation, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return action.get();
        } catch (RuntimeException ex) {
            meterRegistry.counter(
                            MetricNames.EXTERNAL_API_ERRORS,
                            "service", service,
                            "operation", operation,
                            "error", classifyError(ex))
                    .increment();
            throw ex;
        } finally {
            sample.stop(Timer.builder(MetricNames.EXTERNAL_API_DURATION)
                    .description("External API call duration")
                    .tag("service", service)
                    .tag("operation", operation)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}
