package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalApiDependencyAspectTest {

    private static final String SEARCH_METHOD = "search";
    private static final String PAYLOAD = "payload";
    private static final String UPSTREAM_ERROR = "upstream";
    private static final String CHECKED_MESSAGE = "checked";

    private ExternalApiDependencyAspect aspect;

    private static ProceedingJoinPoint mockJoinPoint(String methodName, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn(OpenLibraryClientStub.class);
        if (result != null) {
            when(joinPoint.proceed()).thenReturn(result);
        }
        return joinPoint;
    }

    @BeforeEach
    void setUp() {
        aspect = new ExternalApiDependencyAspect(new DependencyMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void recordsExternalApiCall() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(SEARCH_METHOD, PAYLOAD);

        assertThat(aspect.observeExternalApi(joinPoint)).isEqualTo(PAYLOAD);
    }

    @Test
    void rethrowsRuntimeExceptions() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(SEARCH_METHOD, null);
        when(joinPoint.proceed()).thenThrow(new RuntimeException(UPSTREAM_ERROR));

        assertThatThrownBy(() -> aspect.observeExternalApi(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(UPSTREAM_ERROR);
    }

    @Test
    void wrapsCheckedExceptions() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(SEARCH_METHOD, null);
        when(joinPoint.proceed()).thenThrow(new Exception(CHECKED_MESSAGE));

        assertThatThrownBy(() -> aspect.observeExternalApi(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(Exception.class);
    }

    @SuppressWarnings("unused")
    private static class OpenLibraryClientStub {

        String search() {
            return null;
        }
    }
}
