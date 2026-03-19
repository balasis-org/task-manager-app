package io.github.balasis.taskmanager.engine.infrastructure.email;

// thin send-email interface. prod = Azure Communication Services, dev = local SMTP.
public interface EmailClient {
    void sendEmail(String to, String subject, String body);
}
