package io.github.balasis.taskmanager.engine.infrastructure.email.service;

import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@AllArgsConstructor
public class SmtpEmailClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailClient.class);

    private final String host;
    private final int port;

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.debug("Sending email to={} subject='{}' via {}:{}", to, subject, host, port);

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));

        Session session = Session.getInstance(props);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("noreply@taskmanager.local"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            log.info("Email sent successfully to={}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to={} via {}:{} — {}", to, host, port, e.getMessage(), e);
        }
    }
}
