package com.whatiread.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.shared.web.ApiPaths;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private static final String CLIENT_CORRELATION_ID = "client-correlation-id";
    private static final String CLIENT_CORRELATION_HEADER_VALUE = "  client-correlation-id  ";
    private static final String CLIENT_MDC_ID = "client-mdc-id";
    private static final String OTEL_TRACE_ID = "otel-trace-id";
    private static final String OTEL_SPAN_ID = "otel-span-id";
    private static final String TRACE_FOR_MDC = "trace-for-mdc";
    private static final String SPAN_FOR_MDC = "span-for-mdc";
    private static final String CORRELATION_ID_TAG = "correlation.id";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Mock
    private ObjectProvider<Tracer> tracerProvider;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter(tracerProvider);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void usesIncomingCorrelationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, CLIENT_CORRELATION_HEADER_VALUE);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(CLIENT_CORRELATION_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void populatesMdcWhenClientProvidesCorrelationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, CLIENT_MDC_ID);

        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo(CLIENT_MDC_ID);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void fallsBackToActiveTraceIdWhenHeaderMissing() throws ServletException, IOException {
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(OTEL_TRACE_ID);
        when(traceContext.spanId()).thenReturn(OTEL_SPAN_ID);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(OTEL_TRACE_ID);
        verify(span).tag(CORRELATION_ID_TAG, OTEL_TRACE_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void generatesCorrelationIdWhenHeaderAndTraceMissing() throws ServletException, IOException {
        when(tracerProvider.getIfAvailable()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).matches(UUID_PATTERN);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void populatesMdcDuringRequestAndClearsAfterward() throws ServletException, IOException {
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(TRACE_FOR_MDC);
        when(traceContext.spanId()).thenReturn(SPAN_FOR_MDC);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo(TRACE_FOR_MDC);
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isEqualTo(TRACE_FOR_MDC);
            assertThat(MDC.get(CorrelationIdFilter.SPAN_ID_MDC_KEY)).isEqualTo(SPAN_FOR_MDC);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.SPAN_ID_MDC_KEY)).isNull();
        verify(span).tag(eq(CORRELATION_ID_TAG), eq(TRACE_FOR_MDC));
    }

    @Test
    void generatesCorrelationIdWhenTracerHasNoActiveSpan() throws ServletException, IOException {
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
        verify(span, never()).tag(any(), any());
    }

    @Test
    void generatesCorrelationIdWhenTraceIdBlank() throws ServletException, IOException {
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("   ");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPaths.BOOKS);
        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).matches(UUID_PATTERN);
    }
}
