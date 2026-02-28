package io.github.balasis.taskmanager.engine.monitoring.aspect;

import io.github.balasis.taskmanager.engine.monitoring.profiling.ProfilingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inner profiling ring — records timing for every layer below the controller.
 *
 * Uses a single wide pointcut that matches all app beans, then classifies each call
 * into its layer at runtime by inspecting the declaring type's package name.
 * This avoids referencing packages outside the monitoring module's dependency graph
 * (engine-core, context-web) in the AOP expression — those packages are only available
 * at runtime when the full application is assembled.
 *
 * Active only when the "benchmark" profile is enabled.
 * Works together with ApiResponseTimes (the outer ring that starts/stops the tree).
 *
 * Layer classification (by package segment):
 *   .repository      → db        (JPA repository calls)
 *   .authorization   → auth      (AuthorizationService, RolePolicyService)
 *   .validation      → validator (GroupValidatorImpl, ResourceDataValidator, etc.)
 *   .redis           → redis     (RedisRateLimitService)
 *   .blob            → blob      (BlobStorageService)
 *   .email           → email     (AzureEmailClient, SmtpEmailClient)
 *   .contentsafety   → safety    (ContentSafetyServiceImpl)
 *   .service         → service   (GroupServiceImpl, UserServiceImpl, etc.)
 *
 * Order(10) ensures this fires INSIDE the controller aspect (Order(1)), so timings nest correctly.
 */
@Aspect
@Component
@Profile("benchmark")
@Order(10)
public class LayerProfilingAspect {

    /**
     * Matches every method in the app namespace except the monitoring module itself.
     * Spring AOP only intercepts Spring-managed beans, so entities, DTOs, and exceptions
     * are naturally excluded (they aren't beans).
     * The layer label is determined at runtime via {@link #classifyLayer}.
     */
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

        // Unrecognized layer (controller, config, interceptor, etc.) — pass through silently
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

    // ── Runtime layer classification ────────────────────────────────────

    /**
     * Maps a package name to its profiling layer label.
     * Order matters: more specific segments (authorization, contentsafety) are checked
     * before broader ones (service) to avoid false matches.
     * Returns null for packages that don't belong to any profiled layer.
     */
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
