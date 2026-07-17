package com.whatiread.shelf.service;

import com.whatiread.config.WhatIReadProperties;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShelfUnlockTokenService {

    private static final long UNLOCK_TTL_HOURS = 8;

    private final SecretKey secretKey;

    public ShelfUnlockTokenService(WhatIReadProperties properties) {
        String secret = properties.security().jwt().secret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String create(UUID userId, UUID shelfId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(SecurityConstants.JWT_CLAIM_TYPE, SecurityConstants.JWT_TYPE_SHELF_UNLOCK)
                .claim(SecurityConstants.JWT_CLAIM_SHELF_ID, shelfId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(UNLOCK_TTL_HOURS * 60L * 60L)))
                .signWith(secretKey)
                .compact();
    }

    public boolean isValid(String token, UUID userId, UUID shelfId) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!SecurityConstants.JWT_TYPE_SHELF_UNLOCK.equals(claims.get(SecurityConstants.JWT_CLAIM_TYPE, String.class))) {
                return false;
            }
            UUID claimUserId = UUID.fromString(claims.getSubject());
            UUID claimShelfId = UUID.fromString(claims.get(SecurityConstants.JWT_CLAIM_SHELF_ID, String.class));
            return claimUserId.equals(userId) && claimShelfId.equals(shelfId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public void requireValid(String token, UUID userId, UUID shelfId) {
        if (!isValid(token, userId, shelfId)) {
            throw new ForbiddenException("PIN required to access this secret shelf");
        }
    }
}
