package com.whatiread.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";

    private final ObjectProvider<Tracer> tracer;

    public CorrelationIdFilter(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        MDC.put(MDC_KEY, correlationId);
        putTraceContextInMdc();
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        tagActiveSpan(correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            MDC.remove(TRACE_ID_MDC_KEY);
            MDC.remove(SPAN_ID_MDC_KEY);
        }
    }

    private void putTraceContextInMdc() {
        Tracer activeTracer = tracer.getIfAvailable();
        if (activeTracer == null || activeTracer.currentSpan() == null || activeTracer.currentSpan().context() == null) {
            return;
        }
        var context = activeTracer.currentSpan().context();
        if (context.traceId() != null) {
            MDC.put(TRACE_ID_MDC_KEY, context.traceId());
        }
        if (context.spanId() != null) {
            MDC.put(SPAN_ID_MDC_KEY, context.spanId());
        }
    }

    private void tagActiveSpan(String correlationId) {
        Tracer activeTracer = tracer.getIfAvailable();
        if (activeTracer != null) {
            var span = activeTracer.currentSpan();
            if (span != null) {
                span.tag("correlation.id", correlationId);
            }
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader(CORRELATION_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        Tracer activeTracer = tracer.getIfAvailable();
        if (activeTracer != null && activeTracer.currentSpan() != null && activeTracer.currentSpan().context() != null) {
            String traceId = activeTracer.currentSpan().context().traceId();
            if (traceId != null && !traceId.isBlank()) {
                return traceId;
            }
        }
        return UUID.randomUUID().toString();
    }
}
