package io.github.balasis.taskmanager.engine.infrastructure.email.config;

import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.email.service.SmtpEmailClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev-h2","dev-mssql"})
public class EmailDevConfig {

    @Bean
    public EmailClient emailDevClient(){
        return new SmtpEmailClient(
                System.getenv("SmtpEmailClientHost"),
                Integer.parseInt(System.getenv("SmtpEmailClientPort"))
        );
    }
}
