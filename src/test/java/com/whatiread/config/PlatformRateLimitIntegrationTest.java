package com.whatiread.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.ProblemTypes;
import com.whatiread.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@TestPropertySource(properties = {
        "whatiread.rate-limit.enabled=true",
        "resilience4j.ratelimiter.configs.read.limitForPeriod=2",
        "resilience4j.ratelimiter.configs.read.limitRefreshPeriod=1m",
        "resilience4j.ratelimiter.configs.read.timeoutDuration=0"
})
class PlatformRateLimitIntegrationTest extends AbstractApiIntegrationTest {

    private static final String TEST_CLIENT_IP = "198.51.100.2";

    private static RequestPostProcessor remoteAddr(String addr) {
        return request -> {
            request.setRemoteAddr(addr);
            return request;
        };
    }

    @Test
    void rateLimitReturnsProblemJsonThroughFullStack() throws Exception {
        mockMvc.perform(get(ApiPaths.STATUS).with(remoteAddr(TEST_CLIENT_IP)))
                .andExpect(status().isOk());
        mockMvc.perform(get(ApiPaths.STATUS).with(remoteAddr(TEST_CLIENT_IP)))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.STATUS).with(remoteAddr(TEST_CLIENT_IP)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.type").value(ProblemTypes.uriString(ProblemTypes.RATE_LIMIT)))
                .andExpect(jsonPath("$.status").value(429));
    }
}
