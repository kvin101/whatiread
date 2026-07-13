package com.whatiread.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.AppHttpHeaders;
import com.whatiread.shared.web.ProblemTypes;
import com.whatiread.shared.web.WebPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMetrics(export = true)
@TestPropertySource(properties = {
        "management.endpoints.web.exposure.include=health,info,prometheus",
        "management.prometheus.metrics.export.enabled=true",
        "whatiread.rate-limit.enabled=false"
})
class PlatformSecurityIntegrationTest extends AbstractApiIntegrationTest {

    private static final String INTERNAL_NETWORK_IP = "10.0.0.5";
    private static final String PUBLIC_NETWORK_IP = "203.0.113.10";
    private static final String HEALTH_STATUS_UP = "UP";
    private static final String JSON_PATH_STATUS = "$.status";
    private static final String X_CONTENT_TYPE_OPTIONS_NOSNIFF = "nosniff";
    private static final String X_FRAME_OPTIONS_DENY = "DENY";
    private static final String CSP_DEFAULT_SRC_SELF = "default-src 'self'";
    private static final String MALFORMED_JSON = "{not valid json";

    private static RequestPostProcessor remoteAddr(String addr) {
        return request -> {
            request.setRemoteAddr(addr);
            return request;
        };
    }

    @Test
    void prometheusAllowedFromInternalNetwork() throws Exception {
        mockMvc.perform(get(WebPaths.ACTUATOR_PROMETHEUS).with(remoteAddr(INTERNAL_NETWORK_IP)))
                .andExpect(status().isOk());
    }

    @Test
    void prometheusDeniedFromPublicNetwork() throws Exception {
        mockMvc.perform(get(WebPaths.ACTUATOR_PROMETHEUS).with(remoteAddr(PUBLIC_NETWORK_IP)))
                .andExpect(status().isForbidden());
    }

    @Test
    void infoAllowedFromInternalNetwork() throws Exception {
        mockMvc.perform(get(WebPaths.ACTUATOR_INFO).with(remoteAddr(INTERNAL_NETWORK_IP)))
                .andExpect(status().isOk());
    }

    @Test
    void infoDeniedFromPublicNetwork() throws Exception {
        mockMvc.perform(get(WebPaths.ACTUATOR_INFO).with(remoteAddr(PUBLIC_NETWORK_IP)))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityHeadersPresentOnApiResponses() throws Exception {
        mockMvc.perform(get(ApiPaths.STATUS).with(remoteAddr("10.0.0.11")))
                .andExpect(status().isOk())
                .andExpect(header().string(AppHttpHeaders.X_CONTENT_TYPE_OPTIONS, X_CONTENT_TYPE_OPTIONS_NOSNIFF))
                .andExpect(header().string(AppHttpHeaders.X_FRAME_OPTIONS, X_FRAME_OPTIONS_DENY))
                .andExpect(header().string(
                        AppHttpHeaders.CONTENT_SECURITY_POLICY,
                        org.hamcrest.Matchers.containsString(CSP_DEFAULT_SRC_SELF)));
    }

    @Test
    void correlationIdEchoedOnApiEndpoint() throws Exception {
        String correlationId = "corr-api-" + UUID.randomUUID();

        mockMvc.perform(get(ApiPaths.STATUS)
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                        .with(remoteAddr("10.0.0.13")))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId));
    }

    @Test
    void malformedJsonReturnsProblemDetail() throws Exception {
        mockMvc.perform(post(ApiPaths.AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemTypes.uriString(ProblemTypes.BAD_REQUEST)));
    }

    @Test
    void healthProbeEndpointsAvailable() throws Exception {
        mockMvc.perform(get(WebPaths.ACTUATOR_HEALTH_LIVENESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_STATUS).value(HEALTH_STATUS_UP));

        mockMvc.perform(get(WebPaths.ACTUATOR_HEALTH_READINESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_STATUS).value(HEALTH_STATUS_UP));
    }
}
