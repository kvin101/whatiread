package com.whatiread.shelf.api;

import java.time.Instant;

public record CreateShelfShareLinkRequest(
        Instant expiresAt
) {
}
