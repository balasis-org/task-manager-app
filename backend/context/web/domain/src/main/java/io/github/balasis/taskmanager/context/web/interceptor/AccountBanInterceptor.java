package io.github.balasis.taskmanager.context.web.interceptor;

import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AccountBanInterceptor implements HandlerInterceptor {

    private final EffectiveCurrentUser effectiveCurrentUser;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("GET".equalsIgnoreCase(request.getMethod())
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId;
        try {
            userId = effectiveCurrentUser.getUserId();
        } catch (Exception e) {
            // not authenticated yet — let JwtInterceptor handle it
            return true;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return true;

        if (user.getAccountBannedUntil() != null && Instant.now().isBefore(user.getAccountBannedUntil())) {
            throw new BusinessRuleException(
                    "Your account is temporarily restricted until " + user.getAccountBannedUntil()
                    + " due to repeated content policy violations. Read-only access is still available.");
        }

        return true;
    }
}
