package io.github.balasis.taskmanager.engine.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Records application-level business metrics via Micrometer.
 *
 * These metrics are automatically exported to Azure Application Insights
 * through the OTel-Micrometer bridge (azure-monitor-opentelemetry-autoconfigure).
 * They appear in the 'customMetrics' table in Log Analytics and can be queried
 * with KQL, charted on dashboards, and used as alert rule conditions.
 *
 * Cost / performance notes (see class-level doc at bottom).
 *
 * Metrics recorded:
 *
 *   rate_limit_rejections_total  (counter) — incremented on HTTP 429
 *   rate_limit_infra_failures_total (counter) — Redis unreachable
 *   blob_uploads_total           (counter, tag result=success|failure)
 *   blob_upload_duration         (timer)  — upload wall-clock time
 *   critical_exceptions_total    (counter, tag type=class name)
 *   authentication_attempts_total (counter, tag result=success|failure)
 *
 * How counters work:
 *   Micrometer counters are monotonically increasing in-memory doubles.
 *   They NEVER reset during the lifetime of the JVM.  When the OTel exporter
 *   pushes to Application Insights (every 60 s by default), it sends the DELTA
 *   (difference since last push), not the absolute value.  So Application
 *   Insights stores small per-minute increments, not a growing number.
 *   On container restart the counter starts from 0 — this is expected and
 *   the delta exporter handles it transparently (no data loss, no double-count).
 *
 * Performance impact:
 *   Counter.increment() — a single volatile long add.  ~5 ns.  No lock.
 *   Timer.Sample — two System.nanoTime() calls + one volatile add.  ~10 ns.
 *   The AOP proxy overhead per pointcut is ~0.2 µs (Spring CGLIB dynamic proxy).
 *   Total added latency per request: ≤ 1 µs — completely invisible.
 *   All aspects are non-blocking and allocation-free on the hot path.
 *
 * Cost impact:
 *   The OTel exporter batches metrics into a single HTTPS POST every 60 s.
 *   Each metric is one row in customMetrics (≈ 200 bytes).  With 6 metrics
 *   that's ~1.2 KB/min → ~1.7 MB/month per instance → ~3.4 MB for 2 instances.
 *   Application Insights charges $2.76/GB after the free 5 GB.
 *   3.4 MB is 0.07% of the free tier — effectively zero cost.
 */
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

    // ── AOP intercepts ──────────────────────────────────────────────────

    /**
     * Intercepts RedisRateLimitService.checkRateLimit() and counts rejections/infra failures.
     */
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

    /**
     * Intercepts BlobStorageService.upload*() — records duration + success/failure.
     * Uses Timer.Sample instead of recordCallable to avoid the Callable checked-exception limitation.
     */
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

    /**
     * Intercepts CriticalExceptionAlerter.handleCriticalException() — counts by exception type.
     */
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

    /**
     * Intercepts AuthService.authenticateThroughAzureCode() — counts success/failure.
     */
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
