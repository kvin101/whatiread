package com.whatiread.config.observability;

import java.util.Collection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class InstrumentedCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final DependencyMetrics dependencyMetrics;

    public InstrumentedCacheManager(CacheManager delegate, DependencyMetrics dependencyMetrics) {
        this.delegate = delegate;
        this.dependencyMetrics = dependencyMetrics;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        return new InstrumentedCache(cache, name, dependencyMetrics);
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
