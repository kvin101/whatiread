package com.whatiread.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.whatiread.config.observability.DependencyMetrics;
import com.whatiread.config.observability.InstrumentedCacheManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
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
    public static final String FRIEND_IDS = "friend-ids";
    public static final String SHELF_BOOK_COUNTS = "shelf-book-counts";
    /**
     * Auth principals use {@link com.whatiread.identity.security.AuthPrincipalCache} directly.
     */
    public static final String AUTH_PRINCIPAL = "auth-principal";

    @Bean
    CacheManager cacheManager(DependencyMetrics dependencyMetrics) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                caffeineCache(BOOK_BY_ID, 5_000, 15),
                caffeineCache(PUBLIC_SHELF, 5_000, 15),
                caffeineCache(OPEN_LIBRARY_SEARCH, 5_000, 15),
                caffeineCache(OPEN_LIBRARY_PREVIEW, 5_000, 15),
                caffeineCache(FRIEND_IDS, 5_000, 5),
                caffeineCache(SHELF_BOOK_COUNTS, 2_000, 10)
        ));
        cacheManager.initializeCaches();
        return new InstrumentedCacheManager(cacheManager, dependencyMetrics);
    }

    private static Cache caffeineCache(String name, int maxSize, int ttlMinutes) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                        .recordStats()
                        .build()
        );
    }
}
