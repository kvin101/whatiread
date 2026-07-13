package com.whatiread.identity.service;

import com.whatiread.identity.api.UserSummaryDto;
import com.whatiread.identity.domain.User;
import java.util.UUID;

/**
 * Cross-module API for resolving users without exposing the identity repository.
 */
public interface UserLookupService {

    void requireExists(UUID userId);

    boolean existsById(UUID userId);

    boolean isActiveAccount(UUID userId);

    boolean isActiveAccount(UUID userId, Long tokenVersion);

    long countUsers();

    java.util.Optional<UUID> findOldestUserId();

    UserSummaryDto getSummary(UUID userId);

    User getPersistenceReference(UUID userId);

    User getByEmail(String email);
}
