package com.whatiread.shared.security;

/**
 * Shared security-related string constants (roles, JWT, bearer tokens).
 */
public final class SecurityConstants {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String JWT_CLAIM_TYPE = "type";
    public static final String JWT_CLAIM_EMAIL = "email";
    public static final String JWT_CLAIM_TOKEN_VERSION = "tv";
    public static final String JWT_TYPE_ACCESS = "access";
    public static final String JWT_TYPE_SHELF_UNLOCK = "shelf_unlock";
    public static final String JWT_CLAIM_SHELF_ID = "sid";

    public static final String SHELF_UNLOCK_HEADER = "X-Shelf-Unlock";

    private SecurityConstants() {
    }

    public static String bearerToken(String token) {
        return BEARER_PREFIX + token;
    }
}
