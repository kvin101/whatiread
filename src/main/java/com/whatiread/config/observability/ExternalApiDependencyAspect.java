package com.whatiread.config.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExternalApiDependencyAspect {

    private final DependencyMetrics dependencyMetrics;

    public ExternalApiDependencyAspect(DependencyMetrics dependencyMetrics) {
        this.dependencyMetrics = dependencyMetrics;
    }

    private static String inferServiceName(String clientName) {
        if (clientName.endsWith("Client")) {
            clientName = clientName.substring(0, clientName.length() - "Client".length());
        }
        return RepositoryMetricTags.inferTable(clientName).replace('_', '-');
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

    @Around("externalApiClients()")
    public Object observeExternalApi(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String service = inferServiceName(signature.getDeclaringType().getSimpleName());
        String operation = signature.getName();
        return dependencyMetrics.recordExternalApiCall(
                service, operation, () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable ex) {
                        return rethrow(ex);
                    }
                });
    }

    @Pointcut("execution(* com.whatiread..integration..*Client.*(..))")
    void externalApiClients() {
    }
}
