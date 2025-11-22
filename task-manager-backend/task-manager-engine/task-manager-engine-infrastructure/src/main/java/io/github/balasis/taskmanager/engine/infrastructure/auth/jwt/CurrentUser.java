package io.github.balasis.taskmanager.engine.infrastructure.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class CurrentUser {
    private String azureId;
}
