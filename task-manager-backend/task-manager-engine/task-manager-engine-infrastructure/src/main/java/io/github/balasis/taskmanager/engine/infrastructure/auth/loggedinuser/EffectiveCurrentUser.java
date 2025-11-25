package io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EffectiveCurrentUser {

    private final UserContext userContext;
    private final org.springframework.context.ApplicationContext ctx;

    public Long getUserId() {
        try {
            CurrentUser currentUser = ctx.getBean(CurrentUser.class);
            Long id = currentUser.getUserId();
            if (id != null) return id;
        } catch (Exception ignored) {}
        return userContext.getUserId();
    }
}