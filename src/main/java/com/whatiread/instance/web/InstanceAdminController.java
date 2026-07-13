package com.whatiread.instance.web;

import com.whatiread.instance.api.UpdateRegistrationRequest;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_INSTANCE)
public class InstanceAdminController {

    private final InstanceSettingsService instanceSettingsService;

    public InstanceAdminController(InstanceSettingsService instanceSettingsService) {
        this.instanceSettingsService = instanceSettingsService;
    }

    @PatchMapping("/registration")
    @PreAuthorize("hasRole('ADMIN')")
    void updateRegistration(@Valid @RequestBody UpdateRegistrationRequest request) {
        instanceSettingsService.setRegistrationEnabled(request.enabled());
    }
}
