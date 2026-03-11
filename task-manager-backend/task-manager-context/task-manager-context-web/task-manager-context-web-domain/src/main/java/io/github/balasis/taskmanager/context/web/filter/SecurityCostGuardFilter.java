package io.github.balasis.taskmanager.context.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cost-guard for the arena security-testing profile.
 *
 * <p>Prevents accidental misuse (e.g. running a stress script against the
 * security arena) by enforcing two limits that are invisible to the
 * single-VU security k6 scripts but trip immediately under load:
 * <ul>
 *   <li>Max concurrent requests (default 3 — all security scripts use 1 VU)</li>
 *   <li>Rolling hourly request budget (default 500 — entire security suite is ~106 requests)</li>
 * </ul>
 *
 * <p>Returns <b>503</b> (not 429) so responses are clearly distinguishable
 * from the Bucket4j 429s that security scripts intentionally test.
 */
@Component
@Profile("prod-arena-security")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityCostGuardFilter extends OncePerRequestFilter {

    private static final long ONE_HOUR_MS = 3_600_000L;

    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final AtomicLong hourlyCount = new AtomicLong(0);
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

    private final int maxConcurrent;
    private final long hourlyBudget;

    public SecurityCostGuardFilter(
            @Value("${app.security-guard.max-concurrent-requests:3}") int maxConcurrent,
            @Value("${app.security-guard.hourly-request-budget:500}") long hourlyBudget) {
        this.maxConcurrent = maxConcurrent;
        this.hourlyBudget = hourlyBudget;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Rolling hourly window reset
        long now = System.currentTimeMillis();
        long start = windowStart.get();
        if (now - start > ONE_HOUR_MS) {
            if (windowStart.compareAndSet(start, now)) {
                hourlyCount.set(0);
            }
        }

        // Hourly budget check
        if (hourlyCount.incrementAndGet() > hourlyBudget) {
            send503(response, "hourly request budget exceeded");
            return;
        }

        // Concurrency check
        int current = inFlight.incrementAndGet();
        try {
            if (current > maxConcurrent) {
                send503(response, "too many concurrent requests (" + current + "/" + maxConcurrent + ")");
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private void send503(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Arena security guard: " + reason
                        + ". If running stress tests, use prod-arena-stress profile instead.\"}");
    }
}
