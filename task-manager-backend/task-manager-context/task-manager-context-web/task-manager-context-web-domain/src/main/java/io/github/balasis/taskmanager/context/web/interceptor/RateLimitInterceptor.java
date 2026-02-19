package io.github.balasis.taskmanager.context.web.interceptor;

import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs <b>before</b> the JWT interceptor so every incoming request
 * (including unauthenticated /auth/** paths) is rate-limited by IP.
 * <p>
 * If no {@link RateLimitService} bean exists (e.g. Redis profile not
 * active) the interceptor silently passes through — no request is blocked.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ObjectProvider<RateLimitService> rateLimitServiceProvider;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        RateLimitService rateLimitService = rateLimitServiceProvider.getIfAvailable();
        if (rateLimitService == null) {
            return true; // no rate limiter configured — allow
        }

        String clientIp = resolveClientIp(request);
        rateLimitService.checkRateLimit(clientIp);
        return true;
    }

    /**
     * Extracts the real client IP, respecting the X-Forwarded-For header
     * that Azure App Service / reverse proxies set.
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // first entry is the original client
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
