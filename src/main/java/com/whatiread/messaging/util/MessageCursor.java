package com.whatiread.messaging.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class MessageCursor {

    private MessageCursor() {
    }

    public record Parts(Instant sentAt, UUID id) {
    }

    public static String encode(Instant sentAt, UUID id) {
        String raw = sentAt.toEpochMilli() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Parts decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid message cursor");
        }
        return new Parts(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
    }
}
