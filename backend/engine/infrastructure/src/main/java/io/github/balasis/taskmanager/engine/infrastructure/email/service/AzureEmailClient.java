package io.github.balasis.taskmanager.engine.infrastructure.email.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// sends email through Azure Communication Services (ACS).
// ACS is Microsoft's cloud email/SMS/voice API — separate from Azure AD.
// uses beginSend() which starts a *long-running operation* (LRO): ACS queues the
// email server-side, handles SMTP retries, DKIM signing, and SPF internally.
// the call returns a SyncPoller but we don't block on it — fire and forget.
// ACS rate limits: 30 messages/minute, 100 messages/hour per sender address.
// the EmailQueueDrainer throttles below those limits to avoid 429s.
public class AzureEmailClient implements io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient {
    private static final Logger logger = LoggerFactory.getLogger(AzureEmailClient.class);
    private final EmailClient emailClient;
    private final String senderAddress;

    public AzureEmailClient(EmailClient emailClient, String senderAddress) {
        this.emailClient = emailClient;
        this.senderAddress = senderAddress;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try{
            emailClient.beginSend(
                new EmailMessage()
                    .setSenderAddress(senderAddress)
                    .setToRecipients(to)
                    .setSubject(subject)
                    .setBodyPlainText(body)
            );

        }catch (Exception e){
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

}
