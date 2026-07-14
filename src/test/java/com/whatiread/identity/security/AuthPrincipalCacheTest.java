package com.whatiread.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.whatiread.identity.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthPrincipalCacheTest {

    @Test
    void cachesAndInvalidatesPrincipal() {
        AuthPrincipalCache cache = new AuthPrincipalCache();
        User user = new User("a@example.com", "alice", "hash", "Alice", "A");
        setId(user, UUID.randomUUID());

        cache.put(user);

        assertThat(cache.get(user.getId())).isPresent();
        cache.invalidate(user.getId());
        assertThat(cache.get(user.getId())).isEmpty();
    }

    @Test
    void cachesAdminUserId() {
        AuthPrincipalCache cache = new AuthPrincipalCache();
        UUID adminId = UUID.randomUUID();

        UUID resolved = cache.resolveAdminUserId(() -> adminId);
        UUID cached = cache.resolveAdminUserId(() -> UUID.randomUUID());

        assertThat(resolved).isEqualTo(adminId);
        assertThat(cached).isEqualTo(adminId);

        cache.invalidateAdminUserId();
        UUID afterInvalidate = cache.resolveAdminUserId(() -> UUID.randomUUID());
        assertThat(afterInvalidate).isNotEqualTo(adminId);
    }

    private static void setId(User user, UUID id) {
        try {
            var field = user.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
