package io.github.balasis.taskmanager.engine.monitoring.aspect;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@AllArgsConstructor
public class ApiResponseTimes {
    private final @Nullable OpenTelemetry openTelemetry;

    @Around("execution(public * io.github.balasis.taskmanager..*Controller.*(..))")
    public Object monitorControllerResponseTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controllerName = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - start;

        System.out.println("Execution time of " + controllerName + "." + methodName + ": " + executionTime + "ms");

        if (openTelemetry != null) {
            var tracer = openTelemetry.getTracer("task-manager-tracer");
            var span = tracer.spanBuilder(controllerName + "." + methodName)
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan();
            try (var scope = span.makeCurrent()) {
                span.setAttribute("execution.time.ms", executionTime);
            } finally {
                span.end();
            }
        }

        return result;
    }

}
