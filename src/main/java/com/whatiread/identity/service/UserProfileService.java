package com.whatiread.identity.service;

import com.whatiread.identity.api.UserProfileDto;
import java.util.UUID;

public interface UserProfileService {

    UserProfileDto getProfile(UUID profileUserId, UUID viewerId);
}
