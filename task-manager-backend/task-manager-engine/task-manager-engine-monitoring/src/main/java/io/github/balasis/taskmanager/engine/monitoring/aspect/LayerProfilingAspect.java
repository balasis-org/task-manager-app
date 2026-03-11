package io.github.balasis.taskmanager.engine.monitoring.aspect;

import io.github.balasis.taskmanager.engine.monitoring.profiling.ProfilingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Inner profiling aspect - records timing for layers below the controller.
// Classifies beans into layers (db, auth, validator, service, etc) by package name.
// Only active under the "benchmark" profile.
// Order(10) so it fires inside the controller aspect at Order(1).
@Aspect
@Component
@Profile("benchmark")
@Order(10)
public class LayerProfilingAspect {

    // matches all app beans except monitoring, bootstrap, config, interceptor, advice, filters
    @Around("execution(* io.github.balasis.taskmanager..*(..)) && " +
            "!within(io.github.balasis.taskmanager.engine.monitoring..*) && " +
            "!within(io.github.balasis.taskmanager..bootstrap..*) && " +
            "!within(io.github.balasis.taskmanager..config..*) && " +
            "!within(io.github.balasis.taskmanager..interceptor..*) && " +
            "!within(io.github.balasis.taskmanager..advice..*) && " +
            "!within(jakarta.servlet.Filter+)")
    public Object profileLayer(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!ProfilingContext.isActive()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String packageName = signature.getDeclaringType().getPackageName();
        String layerLabel = classifyLayer(packageName);

        // not a tracked layer, skip
        if (layerLabel == null) {
            return joinPoint.proceed();
        }

        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        ProfilingContext.pushLayer(layerLabel, className, methodName);
        try {
            return joinPoint.proceed();
        } finally {
            ProfilingContext.popLayer();
        }
    }

    // maps package name to a layer label, returns null if not a tracked layer
    // more specific segments checked first to avoid false matches
    private static String classifyLayer(String packageName) {
        if (packageName.contains(".repository"))    return "db";
        if (packageName.contains(".authorization"))  return "auth";
        if (packageName.contains(".validation"))     return "validator";
        if (packageName.contains(".redis"))          return "redis";
        if (packageName.contains(".blob"))           return "blob";
        if (packageName.contains(".email"))          return "email";
        if (packageName.contains(".contentsafety"))  return "safety";
        if (packageName.contains(".service"))        return "service";
        return null;
    }
}
