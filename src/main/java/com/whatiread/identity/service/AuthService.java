package com.whatiread.identity.service;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.identity.api.LoginRequest;
import com.whatiread.identity.api.RefreshRequest;
import com.whatiread.identity.api.RegisterRequest;
import com.whatiread.identity.api.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(RefreshRequest request);

    UserResponse currentUser();
}
