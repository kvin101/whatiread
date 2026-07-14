package com.whatiread.identity.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.whatiread.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class AuthPrincipalCache {

    private static final String ADMIN_USER_KEY = "admin-user-id";

    private final Cache<UUID, Snapshot> principals = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final Cache<String, UUID> adminUserIds = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .recordStats()
            .build();

    public Optional<Snapshot> get(UUID userId) {
        return Optional.ofNullable(principals.getIfPresent(userId));
    }

    public Snapshot fromUser(User user) {
        return new Snapshot(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getTokenVersion()
        );
    }

    public void put(User user) {
        principals.put(user.getId(), fromUser(user));
    }

    public void invalidate(UUID userId) {
        principals.invalidate(userId);
    }

    public UUID resolveAdminUserId(Supplier<UUID> loader) {
        return adminUserIds.get(ADMIN_USER_KEY, key -> loader.get());
    }

    public void invalidateAdminUserId() {
        adminUserIds.invalidateAll();
    }

    public record Snapshot(
            UUID id,
            String email,
            String passwordHash,
            boolean enabled,
            long tokenVersion
    ) {
    }
}
