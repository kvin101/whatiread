package com.whatiread.identity.security;

import com.whatiread.identity.domain.User;
import com.whatiread.instance.service.InstanceSettingsService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserFactory {

    private final AuthPrincipalCache authPrincipalCache;
    private final InstanceSettingsService instanceSettingsService;

    public AuthenticatedUserFactory(
            AuthPrincipalCache authPrincipalCache,
            InstanceSettingsService instanceSettingsService
    ) {
        this.authPrincipalCache = authPrincipalCache;
        this.instanceSettingsService = instanceSettingsService;
    }

    public AuthenticatedUser create(User user) {
        authPrincipalCache.put(user);
        return create(authPrincipalCache.fromUser(user));
    }

    public AuthenticatedUser create(AuthPrincipalCache.Snapshot snapshot) {
        UUID adminUserId = authPrincipalCache.resolveAdminUserId(
                instanceSettingsService::resolveEffectiveAdminUserId
        );
        boolean admin = adminUserId != null && adminUserId.equals(snapshot.id());
        return new AuthenticatedUser(snapshot, admin);
    }
}
