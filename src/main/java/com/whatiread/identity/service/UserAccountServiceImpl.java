package com.whatiread.identity.service;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.identity.suggest.UserSearchIndexService;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UserAccountServiceImpl implements UserAccountService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UsernameService usernameService;
    private final UserSearchIndexService userSearchIndexService;
    private final AvatarStorageService avatarStorageService;

    public UserAccountServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            UsernameService usernameService,
            UserSearchIndexService userSearchIndexService,
            AvatarStorageService avatarStorageService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.usernameService = usernameService;
        this.userSearchIndexService = userSearchIndexService;
        this.avatarStorageService = avatarStorageService;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        if (request.username() != null) {
            String username = usernameService.normalizeAndValidate(request.username());
            usernameService.requireAvailableForUpdate(userId, username);
            user.setUsername(username);
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().trim().isEmpty() ? null : request.phoneNumber().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim().isEmpty() ? null : request.avatarUrl().trim());
        }
        if (request.addressLine1() != null) {
            user.setAddressLine1(blankToNull(request.addressLine1()));
        }
        if (request.addressLine2() != null) {
            user.setAddressLine2(blankToNull(request.addressLine2()));
        }
        if (request.city() != null) {
            user.setCity(blankToNull(request.city()));
        }
        if (request.state() != null) {
            user.setState(blankToNull(request.state()));
        }
        if (request.postalCode() != null) {
            user.setPostalCode(blankToNull(request.postalCode()));
        }
        if (request.country() != null) {
            user.setCountry(blankToNull(request.country()));
        }
        if (request.writer() != null) {
            user.setWriter(request.writer());
        }
        if (request.writerBio() != null) {
            user.setWriterBio(blankToNull(request.writerBio()));
        }
        if (request.acceptRecommendations() != null) {
            user.setAcceptRecommendations(request.acceptRecommendations());
        }
        User saved = userRepository.save(user);
        usernameService.indexUsername(saved.getUsername());
        userSearchIndexService.syncUser(saved);
        return userMapper.toUserResponse(saved);
    }

    @Override
    public UserResponse uploadAvatar(UUID userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String avatarPath = avatarStorageService.store(userId, file);
        user.setAvatarUrl(avatarPath + "?v=" + System.currentTimeMillis());
        User saved = userRepository.save(user);
        userSearchIndexService.syncUser(saved);
        return userMapper.toUserResponse(saved);
    }

    @Override
    public UserResponse removeAvatar(UUID userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        avatarStorageService.delete(userId);
        user.setAvatarUrl(null);
        User saved = userRepository.save(user);
        userSearchIndexService.syncUser(saved);
        return userMapper.toUserResponse(saved);
    }
}
