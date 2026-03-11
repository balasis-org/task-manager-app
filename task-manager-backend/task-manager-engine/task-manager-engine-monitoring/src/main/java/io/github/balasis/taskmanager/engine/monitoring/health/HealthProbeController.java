package io.github.balasis.taskmanager.engine.monitoring.health;

import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Liveness probe - returns 200 once startup gates are cleared, 503 while booting.
// Skipped by JwtInterceptor/RateLimitInterceptor so infra probes hit it without cookies.
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
