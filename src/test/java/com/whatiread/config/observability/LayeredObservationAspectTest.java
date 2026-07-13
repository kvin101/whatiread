package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LayeredObservationAspectTest {

    private static final String FIND_BY_ID = "findById";
    private static final String SEND = "send";
    private static final String PROCESS = "process";
    private static final String BOOK_RESULT = "book";
    private static final String SENT_RESULT = "sent";
    private static final String BOOM_MESSAGE = "boom";
    private static final String CHECKED_MESSAGE = "checked";

    private LayeredObservationAspect aspect;

    private static ProceedingJoinPoint mockJoinPoint(Class<?> declaringType, String methodName, Object result)
            throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn(declaringType);
        if (result != null) {
            when(joinPoint.proceed()).thenReturn(result);
        }
        return joinPoint;
    }

    @BeforeEach
    void setUp() {
        aspect = new LayeredObservationAspect(ObservationRegistry.create());
    }

    @Test
    void observesServiceMethods() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(BookServiceImpl.class, FIND_BY_ID, BOOK_RESULT);

        assertThat(aspect.observeService(joinPoint)).isEqualTo(BOOK_RESULT);
    }

    @Test
    void observesWebSocketMethods() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(ChatStompController.class, SEND, null);
        when(joinPoint.proceed()).thenReturn(SENT_RESULT);

        assertThat(aspect.observeWebSocket(joinPoint)).isEqualTo(SENT_RESULT);
    }

    @Test
    void observesScheduledMethods() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(OutboxEventProcessor.class, PROCESS, 3);
        when(joinPoint.proceed()).thenReturn(3);

        assertThat(aspect.observeScheduled(joinPoint)).isEqualTo(3);
    }

    @Test
    void rethrowsRuntimeExceptions() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(BookServiceImpl.class, FIND_BY_ID, null);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException(BOOM_MESSAGE));

        assertThatThrownBy(() -> aspect.observeService(joinPoint))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrapsCheckedExceptions() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(BookServiceImpl.class, FIND_BY_ID, null);
        when(joinPoint.proceed()).thenThrow(new Exception(CHECKED_MESSAGE));

        assertThatThrownBy(() -> aspect.observeService(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(Exception.class);
    }

    @SuppressWarnings("unused")
    private static class BookServiceImpl {

        Object findById() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class ChatStompController {

        Object send() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class OutboxEventProcessor {

        Object process() {
            return null;
        }
    }
}
