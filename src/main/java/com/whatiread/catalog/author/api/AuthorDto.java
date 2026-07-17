package com.whatiread.catalog.author.api;

import java.util.UUID;

public record AuthorDto(
        UUID id,
        String slug,
        String name,
        String bio,
        String photoUrl,
        String openLibraryAuthorId
) {
}
