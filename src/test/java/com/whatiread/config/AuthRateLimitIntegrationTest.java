package com.whatiread.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.ProblemTypes;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@TestPropertySource(properties = {
        "whatiread.rate-limit.enabled=true",
        "resilience4j.ratelimiter.configs.strict-auth.limitForPeriod=1",
        "resilience4j.ratelimiter.configs.strict-auth.limitRefreshPeriod=1m",
        "resilience4j.ratelimiter.configs.strict-auth.timeoutDuration=0"
})
class AuthRateLimitIntegrationTest extends AbstractApiIntegrationTest {

    private static final String TEST_CLIENT_IP = "198.51.100.11";
    private static final String EMPTY_JSON = "{}";
    private static final String LOGIN_PATH = "/login";

    private static RequestPostProcessor remoteAddr(String addr) {
        return request -> {
            request.setRemoteAddr(addr);
            return request;
        };
    }

    @Test
    void authTierReturns429WhenExceeded() throws Exception {
        mockMvc.perform(post(ApiPaths.AUTH + LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_JSON)
                        .with(remoteAddr(TEST_CLIENT_IP)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(ApiPaths.AUTH + LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_JSON)
                        .with(remoteAddr(TEST_CLIENT_IP)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.type").value(ProblemTypes.uriString(ProblemTypes.RATE_LIMIT)));
    }
}
