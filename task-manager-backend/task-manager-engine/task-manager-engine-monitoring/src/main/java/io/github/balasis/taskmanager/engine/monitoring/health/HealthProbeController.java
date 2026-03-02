package io.github.balasis.taskmanager.engine.monitoring.health;

import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight liveness probe for Azure Front Door health probes.
 *
 * Returns 200 if the application is fully started (both startup gates cleared),
 * or 503 during startup. Does NOT trigger the expensive custom HealthIndicators
 * (Redis PING, Blob Storage list) — those live at /actuator/health and are used
 * by App Service Health Check (internal) for deep readiness verification.
 *
 * This endpoint is excluded from JwtInterceptor and RateLimitInterceptor in
 * WebConfig so that Azure infrastructure probes can reach it without cookies.
 *
 * FD health probes hit the App Service origin directly (bypassing WAF), so this
 * endpoint is reachable by FD. Through FD client traffic, it maps to /api/health
 * which is blocked by a WAF custom rule to prevent external abuse.
 */
@RestController
@RequiredArgsConstructor
public class HealthProbeController {

    private final StartupGate startupGate;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        if (startupGate.isReady()) {
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(503).body("Starting");
    }
}
