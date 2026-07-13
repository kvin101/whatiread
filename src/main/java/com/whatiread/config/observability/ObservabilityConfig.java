package com.whatiread.config.observability;

import com.whatiread.shared.web.WebPaths;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
@EnableAspectJAutoProxy
public class ObservabilityConfig {

    private static final String[] TRACING_EXCLUDED_PREFIXES = {
            WebPaths.ACTUATOR_HEALTH,
            WebPaths.ACTUATOR_PROMETHEUS,
            WebPaths.ACTUATOR_INFO
    };

    /**
     * Skip creating server observations (and trace spans) for low-value actuator endpoints. Collector tail_sampling also drops these paths as a
     * safety net.
     */
    @Bean
    ObservationPredicate skipLowValueActuatorObservations() {
        return (name, context) -> {
            if (!MetricNames.HTTP_SERVER_REQUESTS.equals(name) || !(context instanceof ServerRequestObservationContext serverContext)) {
                return true;
            }
            String path = serverContext.getCarrier().getRequestURI();
            for (String prefix : TRACING_EXCLUDED_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return false;
                }
            }
            return true;
        };
    }
}
