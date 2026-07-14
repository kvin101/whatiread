package com.whatiread.instance.service;

import com.whatiread.config.WhatIReadProperties;
import com.whatiread.identity.security.AuthPrincipalCache;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.instance.domain.InstanceSetting;
import com.whatiread.instance.repository.InstanceSettingRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstanceSettingsServiceImpl implements InstanceSettingsService {

    static final String KEY_SETUP_COMPLETE = "setup_complete";
    static final String KEY_REGISTRATION_ENABLED = "registration_enabled";
    static final String KEY_ADMIN_USER_ID = "admin_user_id";

    private final InstanceSettingRepository repository;
    private final UserLookupService userLookupService;
    private final WhatIReadProperties properties;
    private final AuthPrincipalCache authPrincipalCache;

    public InstanceSettingsServiceImpl(
            InstanceSettingRepository repository,
            UserLookupService userLookupService,
            WhatIReadProperties properties,
            AuthPrincipalCache authPrincipalCache
    ) {
        this.repository = repository;
        this.userLookupService = userLookupService;
        this.properties = properties;
        this.authPrincipalCache = authPrincipalCache;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return userLookupService.countUsers() == 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRegistrationEnabled() {
        String stored = get(KEY_REGISTRATION_ENABLED);
        if (stored != null) {
            return Boolean.parseBoolean(stored);
        }
        return properties.registration().enabled();
    }

    @Override
    public void setRegistrationEnabled(boolean enabled) {
        set(KEY_REGISTRATION_ENABLED, Boolean.toString(enabled));
    }

    @Override
    public void markSetupComplete() {
        set(KEY_SETUP_COMPLETE, "true");
        if (get(KEY_REGISTRATION_ENABLED) == null) {
            setRegistrationEnabled(properties.registration().enabled());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getAdminUserId() {
        String stored = get(KEY_ADMIN_USER_ID);
        return stored != null ? UUID.fromString(stored) : null;
    }

    @Override
    public void setAdminUserId(UUID userId) {
        set(KEY_ADMIN_USER_ID, userId.toString());
        authPrincipalCache.invalidateAdminUserId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveEffectiveAdminUserId() {
        return resolveAdminUserId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInstanceAdmin(UUID userId) {
        UUID adminUserId = resolveAdminUserId();
        return adminUserId != null && adminUserId.equals(userId);
    }

    private UUID resolveAdminUserId() {
        UUID stored = getAdminUserId();
        if (stored != null && userLookupService.existsById(stored)) {
            return stored;
        }
        return userLookupService.findOldestUserId().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String get(String key) {
        return repository.findById(key).map(InstanceSetting::getValue).orElse(null);
    }

    @Override
    public void set(String key, String value) {
        InstanceSetting setting = repository.findById(key)
                .orElseGet(() -> new InstanceSetting(key, value));
        setting.setValue(value);
        repository.save(setting);
    }
}
