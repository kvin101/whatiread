package com.whatiread.identity.web;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.identity.api.LoginRequest;
import com.whatiread.identity.api.RefreshRequest;
import com.whatiread.identity.api.RegisterRequest;
import com.whatiread.identity.api.UsernameAvailabilityResponse;
import com.whatiread.identity.service.AuthService;
import com.whatiread.identity.service.UsernameService;
import com.whatiread.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AUTH)
public class AuthController {

    private final AuthService authService;
    private final UsernameService usernameService;

    public AuthController(AuthService authService, UsernameService usernameService) {
        this.authService = authService;
        this.usernameService = usernameService;
    }

    @GetMapping("/username/available")
    UsernameAvailabilityResponse usernameAvailable(@RequestParam("username") String username) {
        return usernameService.checkAvailability(username, null);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }
}
