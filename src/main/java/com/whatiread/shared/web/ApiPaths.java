package com.whatiread.shared.web;

/**
 * REST API path prefixes and resource roots. Path variables and query strings are appended at call sites.
 */
public final class ApiPaths {

    public static final String V1 = "/api/v1";
    public static final String V1_PREFIX = V1 + "/";

    public static final String AUTH = V1 + "/auth";
    public static final String SETUP = V1 + "/setup";
    public static final String STATUS = V1 + "/status";
    public static final String BOOKS = V1 + "/books";
    public static final String BOOKS_SEARCH = BOOKS + "/search";
    public static final String LIBRARY = V1 + "/library";
    public static final String SHELVES = V1 + "/shelves";
    public static final String FRIENDS = V1 + "/friends";
    public static final String COMMENTS = V1 + "/comments";
    public static final String GOALS = V1 + "/goals";
    public static final String ME = V1 + "/me";
    public static final String USERS = V1 + "/users";
    public static final String CONVERSATIONS = V1 + "/conversations";
    public static final String RECOMMENDATIONS = V1 + "/recommendations";
    public static final String ADMIN_INSTANCE = V1 + "/admin/instance";
    public static final String ADMIN_USERS = V1 + "/admin/users";
    public static final String PUBLIC = V1 + "/public";
    public static final String PUBLIC_USER_SHELVES = PUBLIC + "/users/{ownerId}/shelves";
    public static final String PUBLIC_SHELF_SHARE = PUBLIC + "/shelves/share";
    public static final String IMPORT = V1 + "/import";
    public static final String IMPORT_GOODREADS = IMPORT + "/goodreads";
    public static final String EXPORT = V1 + "/export";

    private ApiPaths() {
    }

    public static String v1(String suffix) {
        if (!suffix.startsWith("/")) {
            throw new IllegalArgumentException("suffix must start with /");
        }
        return V1 + suffix;
    }
}
