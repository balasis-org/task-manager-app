package io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser;

import org.springframework.stereotype.Component;

// thread-local user ID for non-request contexts (DataLoader, scheduled tasks).
// callers must set() before use and clear() after to avoid leaking across threads.
@Component
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public void setUserId(Long id) {
        USER_ID.set(id);
    }

    public Long getUserId() {
        return USER_ID.get();
    }

    public void clear() {
        USER_ID.remove();
    }
}
