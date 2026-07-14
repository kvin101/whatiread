package com.whatiread.shared.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class KeysetCursor {

    private KeysetCursor() {
    }

    public record Parts(Instant timestamp, UUID id) {
        public Instant sentAt() {
            return timestamp;
        }

        public Instant updatedAt() {
            return timestamp;
        }
    }

    public static String encode(Instant timestamp, UUID id) {
        String raw = timestamp.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Parts decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Parts(null, null);
        }
        String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = decoded.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid keyset cursor");
        }
        return new Parts(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
    }
}
