package com.whatiread.identity.api;

import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String displayName,
        String firstName,
        String lastName,
        String avatarUrl,
        boolean writer,
        String writerBio,
        boolean friend,
        boolean self,
        boolean blocked,
        boolean blockedByViewer
) {
}
