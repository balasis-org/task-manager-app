package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.config;

import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ModerationResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// arena stress profile: skip moderation entirely so load tests don't hit the API
@Configuration
@Profile("prod-arena-stress")
public class NoOpContentSafetyConfig {

    @Bean
    public ContentSafetyService contentSafetyService() {
        return input -> ModerationResult.safe();
    }
}
