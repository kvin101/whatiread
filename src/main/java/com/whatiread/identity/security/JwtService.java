package com.whatiread.identity.security;

import com.whatiread.config.WhatIReadProperties;
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

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(WhatIReadProperties properties) {
        this.secretKey = buildKey(properties.security().jwt().secret());
        this.accessTtlMinutes = properties.security().jwt().accessTtlMinutes();
        this.refreshTtlDays = properties.security().jwt().refreshTtlDays();
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(UUID userId, String email, long tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(SecurityConstants.JWT_CLAIM_EMAIL, email)
                .claim(SecurityConstants.JWT_CLAIM_TYPE, SecurityConstants.JWT_TYPE_ACCESS)
                .claim(SecurityConstants.JWT_CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlMinutes * 60L)))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshTokenValue() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plusSeconds(refreshTtlDays * 24L * 60L * 60L);
    }

    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtClaims(
                UUID.fromString(claims.getSubject()),
                claims.get(SecurityConstants.JWT_CLAIM_EMAIL, String.class),
                claims.get(SecurityConstants.JWT_CLAIM_TYPE, String.class),
                claims.get(SecurityConstants.JWT_CLAIM_TOKEN_VERSION, Long.class)
        );
    }

    public record JwtClaims(UUID userId, String email, String type, Long tokenVersion) {
    }
}
