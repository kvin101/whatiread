package com.whatiread.identity.web;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.identity.service.AuthService;
import com.whatiread.identity.service.UserAccountService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ME)
public class UserController {

    private final AuthService authService;
    private final UserAccountService userAccountService;

    public UserController(AuthService authService, UserAccountService userAccountService) {
        this.authService = authService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    UserResponse me() {
        return authService.currentUser();
    }

    @PatchMapping
    UserResponse updateMe(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userAccountService.updateProfile(userId, request);
    }
}
