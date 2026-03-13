package io.github.balasis.taskmanager.engine.monitoring.aspect;

import io.github.balasis.taskmanager.engine.monitoring.profiling.ProfilingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Outer profiling aspect - wraps controller methods, starts the ProfilingContext,
// and prints the full timing breakdown when done.
// Only active under the "benchmark" profile.
// Order(1) so it fires outside the layer aspect at Order(10).
@Aspect
@Component
@Profile("benchmark")
@Order(1)
public class ApiResponseTimes {
    private static final Logger logger = LoggerFactory.getLogger(ApiResponseTimes.class);

    @Around("execution(public * io.github.balasis.taskmanager..*Controller.*(..))")
    public Object profileControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controllerName = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // Start the profiling tree for this request
        ProfilingContext.begin(controllerName, methodName);

        try {
            return joinPoint.proceed();
        } finally {
            // Collect and print the full tree
            String tree = ProfilingContext.end();
            if (tree != null) {
                logger.info(tree);
            }
        }
    }
}
