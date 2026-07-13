package com.whatiread.instance.web;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.instance.api.SetupAdminRequest;
import com.whatiread.instance.api.SetupRequiredDto;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.instance.service.SetupService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SETUP)
public class SetupController {

    private final InstanceSettingsService instanceSettingsService;
    private final SetupService setupService;

    public SetupController(InstanceSettingsService instanceSettingsService, SetupService setupService) {
        this.instanceSettingsService = instanceSettingsService;
        this.setupService = setupService;
    }

    @GetMapping("/required")
    SetupRequiredDto required() {
        return new SetupRequiredDto(
                instanceSettingsService.isSetupRequired(),
                instanceSettingsService.isRegistrationEnabled()
        );
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse createAdmin(@Valid @RequestBody SetupAdminRequest request) {
        return setupService.createAdmin(request);
    }
}
