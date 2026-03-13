package io.github.balasis.taskmanager.engine.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// Records app-level business metrics (rate limit rejections, blob uploads,
// critical exceptions, auth attempts) using Micrometer counters/timers.
// Exported to App Insights via the OTel-Micrometer bridge.
@Aspect
@Component
public class ApplicationMetrics {

    private final MeterRegistry registry;

    private final Counter rateLimitRejections;
    private final Counter rateLimitInfraFailures;
    private final Counter blobUploadSuccess;
    private final Counter blobUploadFailure;
    private final Timer   blobUploadDuration;
    private final Counter authSuccess;
    private final Counter authFailure;

    public ApplicationMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.rateLimitRejections = Counter.builder("rate_limit_rejections_total")
                .description("Total rate-limit rejections (HTTP 429)")
                .register(registry);

        this.rateLimitInfraFailures = Counter.builder("rate_limit_infra_failures_total")
                .description("Rate limiter infrastructure failures (Redis unreachable)")
                .register(registry);

        this.blobUploadSuccess = Counter.builder("blob_uploads_total")
                .tag("result", "success")
                .description("Successful blob uploads")
                .register(registry);

        this.blobUploadFailure = Counter.builder("blob_uploads_total")
                .tag("result", "failure")
                .description("Failed blob uploads")
                .register(registry);

        this.blobUploadDuration = Timer.builder("blob_upload_duration")
                .description("Blob upload wall-clock time")
                .register(registry);

        this.authSuccess = Counter.builder("authentication_attempts_total")
                .tag("result", "success")
                .description("Successful authentication attempts")
                .register(registry);

        this.authFailure = Counter.builder("authentication_attempts_total")
                .tag("result", "failure")
                .description("Failed authentication attempts")
                .register(registry);
    }

    // --- AOP intercepts ---

    // counts rate limit rejections and infra failures
    @AfterThrowing(
            pointcut = "execution(* io.github.balasis.taskmanager.engine.infrastructure.redis.service.RedisRateLimitService.checkRateLimit(..))",
            throwing = "ex"
    )
    public void countRateLimitOutcome(Exception ex) {
        String exName = ex.getClass().getSimpleName();
        if ("RateLimitExceededException".equals(exName)) {
            rateLimitRejections.increment();
        } else if ("CriticalInfrastructureException".equals(exName)) {
            rateLimitInfraFailures.increment();
        }
    }

    // blob upload duration + success/failure tracking
    @Around("execution(* io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService.upload*(..))")
    public Object measureBlobUpload(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        try {
            Object result = joinPoint.proceed();
            blobUploadSuccess.increment();
            return result;
        } catch (Throwable t) {
            blobUploadFailure.increment();
            throw t;
        } finally {
            sample.stop(blobUploadDuration);
        }
    }

    // counts critical exceptions by type
    @Around("execution(* io.github.balasis.taskmanager.engine.monitoring.alert.CriticalExceptionAlerter.handleCriticalException(..))")
    public Object countCritical(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] != null) {
            String type = args[0].getClass().getSimpleName();
            Counter.builder("critical_exceptions_total")
                    .tag("type", type)
                    .description("Critical exception occurrences by type")
                    .register(registry)
                    .increment();
        }
        return joinPoint.proceed();
    }

    // auth success / failure counter
    @Around("execution(* io.github.balasis.taskmanager..auth.AuthService.authenticateThroughAzureCode(..))")
    public Object countAuthAttempt(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            authSuccess.increment();
            return result;
        } catch (Throwable t) {
            authFailure.increment();
            throw t;
        }
    }
}
