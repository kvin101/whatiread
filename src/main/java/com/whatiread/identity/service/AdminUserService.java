package com.whatiread.identity.service;

import com.whatiread.identity.api.AdminCreateUserRequest;
import com.whatiread.identity.api.AdminUserDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserDto> listUsers(String query, Pageable pageable);

    AdminUserDto createUser(AdminCreateUserRequest request, UUID actingAdminId);

    AdminUserDto resetPassword(UUID userId, String newPassword, UUID actingAdminId);

    AdminUserDto setEnabled(UUID userId, boolean enabled, UUID actingAdminId);

    void deleteUser(UUID userId, UUID actingAdminId);
}
