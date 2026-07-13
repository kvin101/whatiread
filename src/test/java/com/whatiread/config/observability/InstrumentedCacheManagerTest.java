package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class InstrumentedCacheManagerTest {

    private static final String BOOKS_CACHE = "books";
    private static final String USERS_CACHE = "users";
    private static final String MISSING_CACHE = "missing";

    private CacheManager delegate;
    private InstrumentedCacheManager manager;

    @BeforeEach
    void setUp() {
        delegate = mock(CacheManager.class);
        manager = new InstrumentedCacheManager(delegate, new DependencyMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void wrapsExistingCaches() {
        Cache raw = mock(Cache.class);
        when(delegate.getCache(BOOKS_CACHE)).thenReturn(raw);

        Cache wrapped = manager.getCache(BOOKS_CACHE);

        assertThat(wrapped).isInstanceOf(InstrumentedCache.class);
        assertThat(wrapped.getName()).isEqualTo(raw.getName());
    }

    @Test
    void returnsNullWhenDelegateHasNoCache() {
        when(delegate.getCache(MISSING_CACHE)).thenReturn(null);

        assertThat(manager.getCache(MISSING_CACHE)).isNull();
    }

    @Test
    void delegatesCacheNames() {
        when(delegate.getCacheNames()).thenReturn(List.of(BOOKS_CACHE, USERS_CACHE));

        assertThat(manager.getCacheNames()).containsExactly(BOOKS_CACHE, USERS_CACHE);
    }
}
