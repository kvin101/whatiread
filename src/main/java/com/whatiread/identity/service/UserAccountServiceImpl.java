package com.whatiread.identity.service;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserAccountServiceImpl implements UserAccountService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserAccountServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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
        return userMapper.toUserResponse(saved);
    }
}
