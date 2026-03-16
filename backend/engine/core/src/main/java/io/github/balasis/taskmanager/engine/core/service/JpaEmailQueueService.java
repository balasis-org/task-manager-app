package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.EmailOutbox;
import io.github.balasis.taskmanager.engine.core.repository.EmailOutboxRepository;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"prod-h2", "prod-azuresql", "dev-h2", "dev-mssql", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class JpaEmailQueueService implements EmailQueueService {

    private final EmailOutboxRepository outboxRepository;

    @Override
    @Transactional
    public void enqueue(String to, String subject, String body) {
        outboxRepository.save(EmailOutbox.builder()
                .toAddress(to)
                .subject(subject)
                .body(body)
                .status("PENDING")
                .retryCount(0)
                .build());
    }
}
