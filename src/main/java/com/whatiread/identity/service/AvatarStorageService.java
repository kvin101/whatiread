package com.whatiread.identity.service;

import com.whatiread.config.WhatIReadProperties;
import com.whatiread.shared.web.ApiPaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final Path directory;

    public AvatarStorageService(WhatIReadProperties properties) throws IOException {
        this.directory = Path.of(properties.avatars().directory()).toAbsolutePath().normalize();
        Files.createDirectories(directory);
    }

    public String store(UUID userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image must be 2 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WebP images are accepted");
        }
        String extension = extensionFor(contentType);
        deleteExisting(userId);
        Path target = directory.resolve(userId + extension);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return ApiPaths.PUBLIC_AVATAR.formatted(userId);
    }

    public void delete(UUID userId) throws IOException {
        deleteExisting(userId);
    }

    public Optional<StoredAvatar> load(UUID userId) throws IOException {
        for (String extension : new String[]{".jpg", ".png", ".webp"}) {
            Path path = directory.resolve(userId + extension);
            if (Files.isRegularFile(path)) {
                return Optional.of(new StoredAvatar(Files.readAllBytes(path), mediaTypeFor(extension)));
            }
        }
        return Optional.empty();
    }

    private void deleteExisting(UUID userId) throws IOException {
        for (String extension : new String[]{".jpg", ".png", ".webp"}) {
            Path path = directory.resolve(userId + extension);
            Files.deleteIfExists(path);
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private static String mediaTypeFor(String extension) {
        return switch (extension) {
            case ".png" -> MediaType.IMAGE_PNG_VALUE;
            case ".webp" -> "image/webp";
            default -> MediaType.IMAGE_JPEG_VALUE;
        };
    }

    public record StoredAvatar(byte[] bytes, String contentType) {
    }
}
