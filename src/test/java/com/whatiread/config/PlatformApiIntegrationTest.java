package com.whatiread.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.AppHttpHeaders;
import com.whatiread.shared.web.WebPaths;
import com.whatiread.support.AbstractApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PlatformApiIntegrationTest extends AbstractApiIntegrationTest {


    private static final String STATUS = "$.status";
    private static final String ID = "$.id";

    @Test
    void healthAndMetricsEndpoints() throws Exception {
        mockMvc.perform(get(WebPaths.ACTUATOR_HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("UP"));

        mockMvc.perform(get(ApiPaths.STATUS))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS).value("ok"));
    }

    @Test
    void idempotencyKeyReplaysPostResponse() throws Exception {
        AuthSession user = registerUser();
        String idempotencyKey = "idem-" + UUID.randomUUID();

        String first = mockMvc.perform(post(ApiPaths.BOOKS)
                        .header(AppHttpHeaders.IDEMPOTENCY_KEY, idempotencyKey)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Idempotent Title",
                                  "authors": ["Author"],
                                  "pageCount": 100
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstId = com.jayway.jsonpath.JsonPath.read(first, ID);

        mockMvc.perform(post(ApiPaths.BOOKS)
                        .header(AppHttpHeaders.IDEMPOTENCY_KEY, idempotencyKey)
                        .with(bearer(user.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Different Title",
                                  "authors": ["Other"],
                                  "pageCount": 50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(ID).value(firstId))
                .andExpect(jsonPath("$.title").value("Idempotent Title"));
    }

    @Test
    void correlationIdEchoedInResponse() throws Exception {
        String correlationId = "corr-" + UUID.randomUUID();

        mockMvc.perform(get(WebPaths.ACTUATOR_HEALTH)
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId));
    }
}
