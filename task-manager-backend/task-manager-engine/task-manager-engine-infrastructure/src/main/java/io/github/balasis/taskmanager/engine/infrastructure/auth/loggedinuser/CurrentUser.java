package io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class CurrentUser {
    private Long userId;
}
