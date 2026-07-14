package com.whatiread.identity.service;

import com.whatiread.identity.api.UserSummaryDto;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserLookupServiceImpl implements UserLookupService {

    private final UserRepository userRepository;

    public UserLookupServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static UserSummaryDto toSummary(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    @Override
    public void requireExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    @Override
    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public boolean isActiveAccount(UUID userId) {
        return isActiveAccount(userId, null);
    }

    @Override
    public boolean isActiveAccount(UUID userId, Long tokenVersion) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .filter(user -> user.isTokenVersionValid(tokenVersion))
                .isPresent();
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public java.util.Optional<UUID> findOldestUserId() {
        return userRepository.findFirstByOrderByCreatedAtAsc().map(User::getId);
    }

    @Override
    public UserSummaryDto getSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toSummary(user);
    }

    @Override
    @Transactional
    public User getPersistenceReference(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
