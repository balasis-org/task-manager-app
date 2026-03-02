package io.github.balasis.taskmanager.context.web.interceptor;

import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.redis.RateLimitService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String ARENA_PREFIX = "a:";

    private final RateLimitService rateLimitService;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final Environment environment;

    private String keyPrefix;

    @PostConstruct
    void init() {
        keyPrefix = Arrays.asList(environment.getActiveProfiles()).contains("prod-arena")
                ? ARENA_PREFIX : "";
    }

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

        rateLimitService.checkRateLimit(keyPrefix + userId);
        return true;
    }
}
