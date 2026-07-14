package com.whatiread.identity.web;

import com.whatiread.identity.service.AvatarStorageService;
import com.whatiread.shared.web.ApiPaths;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PUBLIC_AVATARS)
public class AvatarController {

    private final AvatarStorageService avatarStorageService;

    public AvatarController(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/{userId}")
    ResponseEntity<byte[]> avatar(@PathVariable UUID userId) throws IOException {
        return avatarStorageService.load(userId)
                .map(stored -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, stored.contentType())
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                        .body(stored.bytes()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<byte[]>build());
    }
}
