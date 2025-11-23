package io.github.balasis.taskmanager.engine.infrastructure.auth.jwt;


import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

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
