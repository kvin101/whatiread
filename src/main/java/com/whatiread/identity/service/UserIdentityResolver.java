package com.whatiread.identity.service;

import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shared.util.UsernameUtils;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserIdentityResolver {

    private final UserRepository userRepository;

    public UserIdentityResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID resolveUserId(String userRef) {
        if (userRef == null || userRef.isBlank()) {
            throw new ResourceNotFoundException("User not found");
        }
        try {
            return UUID.fromString(userRef);
        } catch (IllegalArgumentException ignored) {
            String username = UsernameUtils.normalize(userRef);
            return userRepository.findByUsernameIgnoreCase(username)
                    .map(user -> user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
    }
}
