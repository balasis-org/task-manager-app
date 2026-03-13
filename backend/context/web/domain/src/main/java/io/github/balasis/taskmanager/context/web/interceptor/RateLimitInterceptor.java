package io.github.balasis.taskmanager.context.web.interceptor;

import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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

        }

        if (userId == null) {
            return true;
        }

        rateLimitService.checkRateLimit(String.valueOf(userId));
        return true;
    }
}
