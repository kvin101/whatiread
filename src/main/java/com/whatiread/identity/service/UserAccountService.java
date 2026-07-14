package com.whatiread.identity.service;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserAccountService {

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserResponse uploadAvatar(UUID userId, MultipartFile file) throws IOException;

    UserResponse removeAvatar(UUID userId) throws IOException;
}
