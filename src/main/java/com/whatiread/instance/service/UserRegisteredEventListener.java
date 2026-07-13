package com.whatiread.instance.service;

import com.whatiread.shared.event.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventListener {

    private final InstanceSettingsService instanceSettingsService;

    public UserRegisteredEventListener(InstanceSettingsService instanceSettingsService) {
        this.instanceSettingsService = instanceSettingsService;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event.firstUser()) {
            instanceSettingsService.markSetupComplete();
            instanceSettingsService.setAdminUserId(event.userId());
        }
    }
}
