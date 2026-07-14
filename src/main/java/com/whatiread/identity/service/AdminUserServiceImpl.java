package com.whatiread.identity.service;

import com.whatiread.identity.api.AdminCreateUserRequest;
import com.whatiread.identity.api.AdminUserDto;
import com.whatiread.identity.api.AdminUserRole;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.RefreshTokenRepository;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.identity.suggest.UserSearchIndexService;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final InstanceSettingsService instanceSettingsService;
    private final UserMapper userMapper;
    private final UsernameService usernameService;
    private final UserSearchIndexService userSearchIndexService;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            InstanceSettingsService instanceSettingsService,
            UserMapper userMapper,
            UsernameService usernameService,
            UserSearchIndexService userSearchIndexService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.instanceSettingsService = instanceSettingsService;
        this.userMapper = userMapper;
        this.usernameService = usernameService;
        this.userSearchIndexService = userSearchIndexService;
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(String query, Pageable pageable) {
        String normalized = normalizeQuery(query);
        Page<User> users = normalized == null
                ? userRepository.findAllByOrderByCreatedAtDesc(pageable)
                : userRepository.searchByEmailOrName(normalized, pageable);
        return users.map(userMapper::toAdminUserDto);
    }

    @Override
    public AdminUserDto createUser(AdminCreateUserRequest request, UUID actingAdminId) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }
        String username = usernameService.normalizeAndValidate(request.username());
        usernameService.requireAvailable(username);
        User user = new User(
                email,
                username,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName() != null ? request.lastName().trim() : null
        );
        userRepository.save(user);
        usernameService.indexUsername(user.getUsername());
        userSearchIndexService.syncUser(user);
        if (request.role() == AdminUserRole.ADMIN) {
            instanceSettingsService.setAdminUserId(user.getId());
        }
        return userMapper.toAdminUserDto(user);
    }

    @Override
    public AdminUserDto resetPassword(UUID userId, String newPassword, UUID actingAdminId) {
        User user = requireUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        refreshTokenRepository.deleteByUser_Id(userId);
        return userMapper.toAdminUserDto(userRepository.save(user));
    }

    @Override
    public AdminUserDto setEnabled(UUID userId, boolean enabled, UUID actingAdminId) {
        User user = requireUser(userId);
        if (!enabled) {
            assertNotSelf(userId, actingAdminId, "Cannot ban your own account");
            assertNotInstanceAdmin(userId, "Cannot ban the instance administrator");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.deleteByUser_Id(userId);
        }
        User saved = userRepository.save(user);
        userSearchIndexService.syncUser(saved);
        return userMapper.toAdminUserDto(saved);
    }

    @Override
    public void deleteUser(UUID userId, UUID actingAdminId) {
        assertNotSelf(userId, actingAdminId, "Cannot delete your own account");
        assertNotInstanceAdmin(userId, "Cannot delete the instance administrator");
        User user = requireUser(userId);
        refreshTokenRepository.deleteByUser_Id(userId);
        user.setDeleted(true);
        userRepository.save(user);
        userSearchIndexService.removeUser(userId);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void assertNotSelf(UUID userId, UUID actingAdminId, String message) {
        if (userId.equals(actingAdminId)) {
            throw new ForbiddenException(message);
        }
    }

    private void assertNotInstanceAdmin(UUID userId, String message) {
        UUID adminUserId = instanceSettingsService.getAdminUserId();
        if (adminUserId != null && adminUserId.equals(userId)) {
            throw new ForbiddenException(message);
        }
    }
}
