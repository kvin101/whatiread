package com.whatiread.config.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatabaseDependencyAspect {

    private final DependencyMetrics dependencyMetrics;

    public DatabaseDependencyAspect(DependencyMetrics dependencyMetrics) {
        this.dependencyMetrics = dependencyMetrics;
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

    @Around("repositoryMethods()")
    public Object observeRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String table = RepositoryMetricTags.inferTable(signature.getDeclaringType().getSimpleName());
        String operation = RepositoryMetricTags.inferOperation(methodName);
        return dependencyMetrics.recordDatabaseQuery(
                operation, table, () -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable ex) {
                        return rethrow(ex);
                    }
                });
    }

    @Pointcut("execution(* com.whatiread..repository..*(..))")
    void repositoryMethods() {
    }
}
