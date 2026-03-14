package io.github.balasis.taskmanager.engine.infrastructure.email.config;

import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod-arena-stress", "prod-arena-security"})
public class NoOpEmailConfig {

    @Bean
    @Qualifier("userEmailClient")
    public EmailClient userEmailClient() {
        return (to, subject, body) -> { };
    }

    @Bean
    @Qualifier("adminEmailClient")
    public EmailClient adminEmailClient() {
        return (to, subject, body) -> { };
    }
}
