package io.github.balasis.taskmanager.engine.infrastructure.email.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;


import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AzureEmailClient implements io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient {
    private final EmailClient emailClient;
    private final SecretClientProvider secretClientProvider;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try{
            emailClient.beginSend(
                new EmailMessage()
                    .setSenderAddress(secretClientProvider.getSecret("TASKMANAGER-EMAIL-SENDER-ADDRESS"))
                    .setToRecipients(to)
                    .setSubject(subject)
                    .setBodyPlainText(body)
            );

        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

}
