package com.whatiread.shared.event;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, boolean firstUser) {
}
