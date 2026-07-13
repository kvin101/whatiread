package com.whatiread.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.config.WhatIReadProperties;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.instance.repository.InstanceSettingRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstanceSettingsServiceImplTest {

    @Mock
    private InstanceSettingRepository repository;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private WhatIReadProperties properties;

    @InjectMocks
    private InstanceSettingsServiceImpl instanceSettingsService;

    private UUID adminId;
    private UUID oldestUserId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        oldestUserId = UUID.randomUUID();
    }

    @Test
    void isInstanceAdminUsesStoredAdminWhenPresent() {
        when(repository.findById(InstanceSettingsServiceImpl.KEY_ADMIN_USER_ID))
                .thenReturn(Optional.of(new com.whatiread.instance.domain.InstanceSetting(
                        InstanceSettingsServiceImpl.KEY_ADMIN_USER_ID, adminId.toString())));
        when(userLookupService.existsById(adminId)).thenReturn(true);

        assertThat(instanceSettingsService.isInstanceAdmin(adminId)).isTrue();
        assertThat(instanceSettingsService.isInstanceAdmin(oldestUserId)).isFalse();
    }

    @Test
    void isInstanceAdminFallsBackToOldestUserWhenStoredAdminMissing() {
        when(repository.findById(InstanceSettingsServiceImpl.KEY_ADMIN_USER_ID)).thenReturn(Optional.empty());
        when(userLookupService.findOldestUserId()).thenReturn(Optional.of(oldestUserId));

        assertThat(instanceSettingsService.isInstanceAdmin(oldestUserId)).isTrue();
        assertThat(instanceSettingsService.isInstanceAdmin(adminId)).isFalse();
    }

    @Test
    void isInstanceAdminFallsBackToOldestUserWhenStoredAdminDeleted() {
        when(repository.findById(InstanceSettingsServiceImpl.KEY_ADMIN_USER_ID))
                .thenReturn(Optional.of(new com.whatiread.instance.domain.InstanceSetting(
                        InstanceSettingsServiceImpl.KEY_ADMIN_USER_ID, adminId.toString())));
        when(userLookupService.existsById(adminId)).thenReturn(false);
        when(userLookupService.findOldestUserId()).thenReturn(Optional.of(oldestUserId));

        assertThat(instanceSettingsService.isInstanceAdmin(oldestUserId)).isTrue();
        assertThat(instanceSettingsService.isInstanceAdmin(adminId)).isFalse();
    }
}
