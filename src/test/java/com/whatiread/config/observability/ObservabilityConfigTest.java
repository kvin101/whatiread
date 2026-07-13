package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.whatiread.shared.web.ApiPaths;
import com.whatiread.shared.web.WebPaths;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;

class ObservabilityConfigTest {

    private final ObservabilityConfig config = new ObservabilityConfig();

    private static ServerRequestObservationContext actuatorContext(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return new ServerRequestObservationContext(request, null);
    }

    @Test
    void skipsLowValueActuatorObservations() {
        ObservationPredicate predicate = config.skipLowValueActuatorObservations();

        assertThat(predicate.test(MetricNames.HTTP_SERVER_REQUESTS, actuatorContext(WebPaths.ACTUATOR_HEALTH))).isFalse();
        assertThat(predicate.test(MetricNames.HTTP_SERVER_REQUESTS, actuatorContext(WebPaths.ACTUATOR_PROMETHEUS))).isFalse();
        assertThat(predicate.test(MetricNames.HTTP_SERVER_REQUESTS, actuatorContext(WebPaths.ACTUATOR_INFO))).isFalse();
        assertThat(predicate.test(MetricNames.HTTP_SERVER_REQUESTS, actuatorContext(ApiPaths.BOOKS))).isTrue();
        assertThat(predicate.test("other.observation", actuatorContext(WebPaths.ACTUATOR_HEALTH))).isTrue();
        assertThat(predicate.test(MetricNames.HTTP_SERVER_REQUESTS, mock(Observation.Context.class))).isTrue();
    }
}
