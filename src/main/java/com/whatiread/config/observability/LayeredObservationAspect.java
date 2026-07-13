package com.whatiread.config.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LayeredObservationAspect {

    private final ObservationRegistry observationRegistry;

    public LayeredObservationAspect(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    private static Object rethrow(Throwable ex) {
        if (ex instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (ex instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException(ex);
    }

    @Around("serviceMethods()")
    public Object observeService(ProceedingJoinPoint joinPoint) throws Throwable {
        return observe(joinPoint, "service", "app.service");
    }

    @Around("webSocketMethods()")
    public Object observeWebSocket(ProceedingJoinPoint joinPoint) throws Throwable {
        return observe(joinPoint, "messaging", "websocket.handler");
    }

    @Around("scheduledMethods()")
    public Object observeScheduled(ProceedingJoinPoint joinPoint) throws Throwable {
        return observe(joinPoint, "scheduler", "scheduler.job");
    }

    @Pointcut("""
            execution(public * com.whatiread..*ServiceImpl.*(..))
            || execution(public * com.whatiread..*Processor.*(..))
            || execution(public * com.whatiread..service..*Service.*(..))
            """)
    void serviceMethods() {
    }

    @Pointcut("execution(public * com.whatiread.messaging.websocket.ChatStompController.*(..))")
    void webSocketMethods() {
    }

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    void scheduledMethods() {
    }

    private Object observe(ProceedingJoinPoint joinPoint, String layer, String prefix) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String observationName = prefix + "." + className + "." + methodName;

        Observation observation = Observation.createNotStarted(observationName, observationRegistry)
                .lowCardinalityKeyValue("layer", layer)
                .lowCardinalityKeyValue("class", className)
                .lowCardinalityKeyValue("method", methodName);

        return observation.observe(() -> proceed(joinPoint, observation));
    }

    private Object proceed(ProceedingJoinPoint joinPoint, Observation observation) {
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            observation.error(ex);
            return rethrow(ex);
        }
    }
}
