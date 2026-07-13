package com.whatiread.shared.web;

/**
 * Non-API web paths (actuator, WebSocket, static assets, OpenAPI).
 */
public final class WebPaths {

    public static final String ACTUATOR = "/actuator";
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_LIVENESS = ACTUATOR_HEALTH + "/liveness";
    public static final String ACTUATOR_HEALTH_READINESS = ACTUATOR_HEALTH + "/readiness";
    public static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus";
    public static final String ACTUATOR_INFO = "/actuator/info";
    public static final String WS = "/ws";
    public static final String STATIC_PREFIX = "/static/";
    public static final String SWAGGER_UI = "/swagger-ui.html";
    public static final String OPENAPI_DOCS = "/v3/api-docs";

    private WebPaths() {
    }
}
