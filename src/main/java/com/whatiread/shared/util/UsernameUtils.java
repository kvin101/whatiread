package com.whatiread.shared.util;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class UsernameUtils {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 30;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{2,29}$");

    private static final Set<String> RESERVED = Set.of(
            "admin", "api", "auth", "login", "register", "setup", "me", "users",
            "library", "shelves", "messages", "friends", "explore", "settings",
            "share", "public", "system", "null", "undefined", "support", "help"
    );

    private UsernameUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Username is required");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static void validate(String username) {
        String normalized = normalize(username);
        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be " + MIN_LENGTH + "–" + MAX_LENGTH + " characters"
            );
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Username must start with a letter and use only lowercase letters, numbers, and underscores"
            );
        }
        if (RESERVED.contains(normalized)) {
            throw new IllegalArgumentException("Username is reserved");
        }
    }

    public static boolean isReserved(String normalized) {
        return RESERVED.contains(normalized.toLowerCase(Locale.ROOT));
    }

    public static String backfillFromUserId(UUID userId) {
        String base = "u" + userId.toString().replace("-", "");
        return base.length() <= MAX_LENGTH ? base : base.substring(0, MAX_LENGTH);
    }
}
