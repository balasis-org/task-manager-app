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

/**
 * Outer profiling ring — wraps every controller method, starts the ProfilingContext tree,
 * waits for the method to finish, then prints the full timing breakdown to the console.
 *
 * This is a pure console profiler — no OpenTelemetry dependency needed.
 * Active only when the "benchmark" profile is enabled.
 *
 * Output example:
 *
 *   ━━━ GroupController.getGroup (total: 47ms) ━━━
 *     service .............. 45ms  (GroupServiceImpl.getGroup)
 *       db ................. 12ms  (GroupRepository.findById)
 *       db .................  8ms  (GroupMembershipRepository.findByGroupIdAndUserId)
 *       auth ...............  2ms  (AuthorizationService.requireAnyRoleIn)
 *     validator ............  1ms  (ResourceDataValidator.validateResourceData)
 *   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * Order(1) ensures this fires OUTSIDE the layer aspects (Order(10)),
 * so the controller is the root of the tree and all inner calls nest under it.
 */
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
