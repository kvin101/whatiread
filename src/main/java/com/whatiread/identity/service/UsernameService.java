package com.whatiread.identity.service;

import com.whatiread.identity.api.UsernameAvailabilityResponse;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.util.UsernameUtils;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UsernameService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{2,29}$");

    private final UserRepository userRepository;
    private final UsernameBloomFilterRegistry bloomFilterRegistry;

    public UsernameService(UserRepository userRepository, UsernameBloomFilterRegistry bloomFilterRegistry) {
        this.userRepository = userRepository;
        this.bloomFilterRegistry = bloomFilterRegistry;
    }

    public String normalizeAndValidate(String raw) {
        String username = UsernameUtils.normalize(raw);
        UsernameUtils.validate(username);
        return username;
    }

    public void requireAvailable(String username) {
        if (isTaken(username, null)) {
            throw new ConflictException("Username already taken");
        }
    }

    public void requireAvailableForUpdate(UUID userId, String username) {
        if (isTaken(username, userId)) {
            throw new ConflictException("Username already taken");
        }
    }

    public UsernameAvailabilityResponse checkAvailability(String raw, UUID excludeUserId) {
        if (raw == null || raw.isBlank()) {
            return new UsernameAvailabilityResponse("", false, false, "Username is required");
        }
        String normalized = UsernameUtils.normalize(raw);
        Optional<String> formatError = formatError(normalized);
        if (formatError.isPresent()) {
            return new UsernameAvailabilityResponse(normalized, false, false, formatError.get());
        }
        if (isCurrentUsername(excludeUserId, normalized)) {
            return new UsernameAvailabilityResponse(normalized, true, true, null);
        }
        if (!bloomFilterRegistry.mightContain(normalized)) {
            return new UsernameAvailabilityResponse(normalized, true, true, null);
        }
        boolean taken = isTaken(normalized, excludeUserId);
        return new UsernameAvailabilityResponse(
                normalized,
                true,
                !taken,
                taken ? "Username already taken" : null
        );
    }

    public void indexUsername(String username) {
        bloomFilterRegistry.register(username);
    }

    private boolean isCurrentUsername(UUID excludeUserId, String normalized) {
        if (excludeUserId == null) {
            return false;
        }
        return userRepository.findById(excludeUserId)
                .map(user -> user.getUsername().equalsIgnoreCase(normalized))
                .orElse(false);
    }

    private boolean isTaken(String username, UUID excludeUserId) {
        return excludeUserId == null
                ? userRepository.existsByUsernameIgnoreCase(username)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludeUserId);
    }

    private static Optional<String> formatError(String normalized) {
        if (normalized.length() < UsernameUtils.MIN_LENGTH || normalized.length() > UsernameUtils.MAX_LENGTH) {
            return Optional.of(
                    "Username must be " + UsernameUtils.MIN_LENGTH + "–" + UsernameUtils.MAX_LENGTH + " characters"
            );
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            return Optional.of(
                    "Username must start with a letter and use only lowercase letters, numbers, and underscores"
            );
        }
        if (UsernameUtils.isReserved(normalized)) {
            return Optional.of("Username is reserved");
        }
        return Optional.empty();
    }
}
