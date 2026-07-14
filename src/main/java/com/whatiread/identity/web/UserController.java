package com.whatiread.identity.web;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.api.UsernameAvailabilityResponse;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.identity.service.AuthService;
import com.whatiread.identity.service.UserAccountService;
import com.whatiread.identity.service.UsernameService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.ME)
public class UserController {

    private final AuthService authService;
    private final UserAccountService userAccountService;
    private final UsernameService usernameService;

    public UserController(
            AuthService authService,
            UserAccountService userAccountService,
            UsernameService usernameService
    ) {
        this.authService = authService;
        this.userAccountService = userAccountService;
        this.usernameService = usernameService;
    }

    @GetMapping("/username/available")
    UsernameAvailabilityResponse usernameAvailable(
            @CurrentUserId UUID userId,
            @RequestParam("username") String username
    ) {
        return usernameService.checkAvailability(username, userId);
    }

    @GetMapping
    UserResponse me() {
        return authService.currentUser();
    }

    @PatchMapping
    UserResponse updateMe(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userAccountService.updateProfile(userId, request);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UserResponse uploadAvatar(@CurrentUserId UUID userId, @RequestPart("file") MultipartFile file) throws IOException {
        return userAccountService.uploadAvatar(userId, file);
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeAvatar(@CurrentUserId UUID userId) throws IOException {
        userAccountService.removeAvatar(userId);
    }
}
