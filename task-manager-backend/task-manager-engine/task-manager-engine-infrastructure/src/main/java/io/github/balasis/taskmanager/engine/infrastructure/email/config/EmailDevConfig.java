package io.github.balasis.taskmanager.engine.infrastructure.email.config;

import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.email.service.SmtpEmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev-h2","dev-mssql","dev-flyway-mssql"})
public class EmailDevConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailDevConfig.class);

    @Bean
    public EmailClient emailDevClient(){
        String host = System.getenv("SmtpEmailClientHost");
        String portStr = System.getenv("SmtpEmailClientPort");

        if (host == null || host.isBlank()) {
            host = "localhost";
            log.warn("SmtpEmailClientHost env var not set — defaulting to 'localhost'");
        }

        int port;
        try {
            port = (portStr != null && !portStr.isBlank()) ? Integer.parseInt(portStr) : 1025;
        } catch (NumberFormatException e) {
            port = 1025;
            log.warn("SmtpEmailClientPort env var invalid ('{}') — defaulting to 1025", portStr);
        }

        log.info("SMTP email client configured: {}:{}", host, port);
        return new SmtpEmailClient(host, port);
    }
}
