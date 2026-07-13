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

class DatabaseDependencyAspectTest {

    private static final String FIND_BY_ID = "findById";
    private static final String SAVE = "save";
    private static final String OK_RESULT = "ok";
    private static final String DB_ERROR = "db error";
    private static final String CHECKED_MESSAGE = "checked";

    private DatabaseDependencyAspect aspect;
    private DependencyMetrics metrics;

    private static ProceedingJoinPoint mockJoinPoint(String methodName, Object result)
            throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn(FakeRepository.class);
        if (result != null) {
            when(joinPoint.proceed()).thenReturn(result);
        }
        return joinPoint;
    }

    @BeforeEach
    void setUp() {
        metrics = new DependencyMetrics(new SimpleMeterRegistry());
        aspect = new DatabaseDependencyAspect(metrics);
    }

    @Test
    void recordsRepositoryCallAndReturnsResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(FIND_BY_ID, OK_RESULT);

        assertThat(aspect.observeRepository(joinPoint)).isEqualTo(OK_RESULT);
    }

    @Test
    void rethrowsRuntimeExceptions() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(FIND_BY_ID, null);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException(DB_ERROR));

        assertThatThrownBy(() -> aspect.observeRepository(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(DB_ERROR);
    }

    @Test
    void wrapsCheckedExceptions() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(SAVE, null);
        when(joinPoint.proceed()).thenThrow(new Exception(CHECKED_MESSAGE));

        assertThatThrownBy(() -> aspect.observeRepository(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(Exception.class);
    }

    @SuppressWarnings("unused")
    private static class FakeRepository {

        String findById() {
            return null;
        }

        void save() {
        }
    }
}
