package com.whatiread.instance.service;

import java.util.UUID;

public interface InstanceSettingsService {

    boolean isSetupRequired();

    boolean isRegistrationEnabled();

    void setRegistrationEnabled(boolean enabled);

    void markSetupComplete();

    UUID getAdminUserId();

    UUID resolveEffectiveAdminUserId();

    void setAdminUserId(UUID userId);

    boolean isInstanceAdmin(UUID userId);

    String get(String key);

    void set(String key, String value);
}
