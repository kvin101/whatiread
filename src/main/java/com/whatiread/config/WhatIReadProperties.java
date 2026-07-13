package com.whatiread.config;

import com.whatiread.shared.web.WebPaths;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "whatiread")
public record WhatIReadProperties(
        Security security,
        Registration registration,
        Cors cors,
        GoogleBooks googleBooks,
        RateLimit rateLimit,
        RequestLogging requestLogging
) {

    public WhatIReadProperties {
        if (rateLimit == null) {
            rateLimit = new RateLimit(true);
        }
        if (requestLogging == null) {
            requestLogging = new RequestLogging(true, true, List.of(WebPaths.ACTUATOR_HEALTH));
        }
    }

    public record Security(Jwt jwt) {
    }

    public record Jwt(
            String secret,
            int accessTtlMinutes,
            int refreshTtlDays
    ) {
    }

    public record Registration(boolean enabled) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record GoogleBooks(String apiKey) {
    }

    public record RateLimit(boolean enabled) {
    }

    public record RequestLogging(
            boolean enabled,
            boolean includeQueryString,
            List<String> excludePaths
    ) {

        public RequestLogging {
            if (excludePaths == null) {
                excludePaths = List.of(WebPaths.ACTUATOR_HEALTH);
            }
        }
    }
}
