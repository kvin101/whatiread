package com.whatiread.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.whatiread.config.observability.DependencyMetrics;
import com.whatiread.config.observability.InstrumentedCacheManager;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

    public static final String BOOK_BY_ID = "books-by-id";
    public static final String PUBLIC_SHELF = "public-shelves";
    public static final String OPEN_LIBRARY_SEARCH = "open-library-search";
    public static final String OPEN_LIBRARY_PREVIEW = "open-library-preview";

    @Bean
    CacheManager cacheManager(DependencyMetrics dependencyMetrics) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                BOOK_BY_ID, PUBLIC_SHELF, OPEN_LIBRARY_SEARCH, OPEN_LIBRARY_PREVIEW);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats());
        return new InstrumentedCacheManager(cacheManager, dependencyMetrics);
    }
}
