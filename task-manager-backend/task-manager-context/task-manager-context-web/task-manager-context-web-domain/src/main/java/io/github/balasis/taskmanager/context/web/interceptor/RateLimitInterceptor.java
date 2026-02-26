package io.github.balasis.taskmanager.context.web.interceptor;

import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs <b>after</b> the JWT interceptor so the authenticated userId
 * is available for rate-limiting keyed by user rather than IP.
 * <p>
 * Redis (and therefore {@link RateLimitService}) is <b>mandatory</b> —
 * the application will not start if no bean is available.
 * Unauthenticated endpoints (excluded in {@code WebConfig}) never reach
 * this interceptor.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final EffectiveCurrentUser effectiveCurrentUser;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        Long userId = null;
        try {
            userId = effectiveCurrentUser.getUserId();
        } catch (Exception ignored) {
            // no authenticated user (public endpoint) — skip rate limiting
        }

        if (userId == null) {
            return true; // unauthenticated request — not rate-limited
        }

        rateLimitService.checkRateLimit(String.valueOf(userId));
        return true;
    }
}
