package io.github.balasis.taskmanager.engine.infrastructure.email;

public interface EmailClient {
    void sendEmail(String to, String subject, String body);
}
