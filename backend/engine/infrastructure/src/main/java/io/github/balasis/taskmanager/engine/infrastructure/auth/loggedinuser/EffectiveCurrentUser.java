package io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// bridge between request scope (CurrentUser) and thread-local (UserContext).
// tries the request-scoped bean first; if outside a request (e.g. scheduler, DataLoader)
// falls back to the thread-local. this way services don't care where the userId came from.
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
