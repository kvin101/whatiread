package com.whatiread.identity.service;

import com.whatiread.identity.api.AdminUserDto;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.instance.service.InstanceSettingsService;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final InstanceSettingsService instanceSettingsService;

    public UserMapper(InstanceSettingsService instanceSettingsService) {
        this.instanceSettingsService = instanceSettingsService;
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getAddressLine1(),
                user.getAddressLine2(),
                user.getCity(),
                user.getState(),
                user.getPostalCode(),
                user.getCountry(),
                user.isWriter(),
                user.getWriterBio(),
                user.isAcceptRecommendations(),
                user.getCreatedAt(),
                instanceSettingsService.isInstanceAdmin(user.getId())
        );
    }

    public AdminUserDto toAdminUserDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                instanceSettingsService.isInstanceAdmin(user.getId()),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
