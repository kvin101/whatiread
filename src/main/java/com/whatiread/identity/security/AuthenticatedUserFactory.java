package com.whatiread.identity.security;

import com.whatiread.identity.domain.User;
import com.whatiread.instance.service.InstanceSettingsService;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserFactory {

    private final InstanceSettingsService instanceSettingsService;

    public AuthenticatedUserFactory(InstanceSettingsService instanceSettingsService) {
        this.instanceSettingsService = instanceSettingsService;
    }

    public AuthenticatedUser create(User user) {
        return new AuthenticatedUser(user, instanceSettingsService.isInstanceAdmin(user.getId()));
    }
}
