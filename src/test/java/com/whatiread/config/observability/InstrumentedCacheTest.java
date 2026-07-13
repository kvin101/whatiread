package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class InstrumentedCacheTest {

    private static final String CACHE_NAME = "books";
    private static final String CACHE_KEY = "key";
    private static final String STORED_VALUE = "value";
    private static final String CACHED_VALUE = "cached";
    private static final String TYPED_VALUE = "typed";
    private static final String LOADED_VALUE = "loaded";
    private static final String CACHE_ERROR_MESSAGE = "cache down";

    private Cache delegate;
    private DependencyMetrics metrics;
    private InstrumentedCache cache;

    @BeforeEach
    void setUp() {
        delegate = mock(Cache.class);
        metrics = new DependencyMetrics(new SimpleMeterRegistry());
        cache = new InstrumentedCache(delegate, CACHE_NAME, metrics);
    }

    @Test
    void delegatesNameAndNativeCache() {
        Object nativeCache = new Object();
        when(delegate.getName()).thenReturn(CACHE_NAME);
        when(delegate.getNativeCache()).thenReturn(nativeCache);

        assertThat(cache.getName()).isEqualTo(CACHE_NAME);
        assertThat(cache.getNativeCache()).isSameAs(nativeCache);
    }

    @Test
    void getValueWrapperDelegatesThroughMetrics() {
        Cache.ValueWrapper wrapper = () -> STORED_VALUE;
        when(delegate.get(CACHE_KEY)).thenReturn(wrapper);

        assertThat(cache.get(CACHE_KEY)).isSameAs(wrapper);
    }

    @Test
    void getWithTypeDelegatesThroughMetrics() {
        when(delegate.get(CACHE_KEY, String.class)).thenReturn(TYPED_VALUE);

        assertThat(cache.get(CACHE_KEY, String.class)).isEqualTo(TYPED_VALUE);
    }

    @Test
    void getWithLoaderUsesHitPathWhenCached() throws Exception {
        Cache.ValueWrapper wrapper = () -> CACHED_VALUE;
        when(delegate.get(CACHE_KEY)).thenReturn(wrapper);

        assertThat(cache.get(CACHE_KEY, () -> LOADED_VALUE)).isEqualTo(CACHED_VALUE);
        verify(delegate).get(CACHE_KEY);
    }

    @Test
    void getWithLoaderLoadsOnMiss() throws Exception {
        when(delegate.get(CACHE_KEY)).thenReturn(null);
        when(delegate.get(eq(CACHE_KEY), any(Callable.class))).thenReturn(LOADED_VALUE);

        Callable<String> loader = () -> LOADED_VALUE;
        assertThat(cache.get(CACHE_KEY, loader)).isEqualTo(LOADED_VALUE);
    }

    @Test
    void putEvictAndClearDelegateThroughMetrics() {
        cache.put(CACHE_KEY, STORED_VALUE);
        cache.evict(CACHE_KEY);
        cache.clear();

        verify(delegate).put(CACHE_KEY, STORED_VALUE);
        verify(delegate).evict(CACHE_KEY);
        verify(delegate).clear();
    }

    @Test
    void propagatesDelegateErrors() {
        when(delegate.get(CACHE_KEY)).thenThrow(new RuntimeException(CACHE_ERROR_MESSAGE));

        assertThatThrownBy(() -> cache.get(CACHE_KEY))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(CACHE_ERROR_MESSAGE);
    }
}
