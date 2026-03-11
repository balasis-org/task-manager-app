package io.github.balasis.taskmanager.engine.infrastructure.contentsafety.config;

import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ContentSafetyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod-arena-stress")
public class NoOpContentSafetyConfig {

    @Bean
    public ContentSafetyService contentSafetyService() {
        return input -> true;
    }
}
