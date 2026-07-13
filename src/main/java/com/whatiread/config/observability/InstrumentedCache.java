package com.whatiread.config.observability;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;

final class InstrumentedCache implements Cache {

    private final Cache delegate;
    private final String cacheName;
    private final DependencyMetrics dependencyMetrics;

    InstrumentedCache(Cache delegate, String cacheName, DependencyMetrics dependencyMetrics) {
        this.delegate = delegate;
        this.cacheName = cacheName;
        this.dependencyMetrics = dependencyMetrics;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        return dependencyMetrics.recordCacheRequest(cacheName, "get", () -> delegate.get(key));
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return dependencyMetrics.recordCacheRequest(cacheName, "get", () -> delegate.get(key, type));
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper cached = delegate.get(key);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            T value = (T) cached.get();
            return dependencyMetrics.recordCacheRequest(cacheName, "get", true, () -> value);
        }
        return dependencyMetrics.recordCacheRequest(cacheName, "get", false, () -> delegate.get(key, valueLoader));
    }

    @Override
    public void put(Object key, Object value) {
        dependencyMetrics.recordCacheRequest(
                cacheName, "put", () -> {
                    delegate.put(key, value);
                    return null;
                });
    }

    @Override
    public void evict(Object key) {
        dependencyMetrics.recordCacheRequest(
                cacheName, "evict", () -> {
                    delegate.evict(key);
                    return null;
                });
    }

    @Override
    public void clear() {
        dependencyMetrics.recordCacheRequest(
                cacheName, "clear", () -> {
                    delegate.clear();
                    return null;
                });
    }
}
