package com.whatiread.messaging.util;

import com.whatiread.shared.util.KeysetCursor;
import java.time.Instant;
import java.util.UUID;

public final class MessageCursor {

    private MessageCursor() {
    }

    public record Parts(Instant sentAt, UUID id) {
    }

    public static String encode(Instant sentAt, UUID id) {
        return KeysetCursor.encode(sentAt, id);
    }

    public static Parts decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        KeysetCursor.Parts parts = KeysetCursor.decode(cursor);
        return new Parts(parts.sentAt(), parts.id());
    }
}
