package com.whatiread.identity.service;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import java.util.UUID;

public interface UserAccountService {

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
