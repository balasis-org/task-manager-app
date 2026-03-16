package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.EmailOutbox;
import io.github.balasis.taskmanager.engine.core.repository.EmailOutboxRepository;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.redis.EmailDrainLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Drains the EmailOutbox table at a rate that respects ACS platform limits
 * (30 messages/minute, 100 messages/hour).  A Redis distributed lock ensures
 * only one application instance drains at a time across scaled-out replicas.
 */
@Service
@Profile({"prod-h2", "prod-azuresql", "dev-h2", "dev-mssql", "dev-flyway-mssql"})
public class EmailQueueDrainer {

    private static final Logger log = LoggerFactory.getLogger(EmailQueueDrainer.class);

    private static final int MAX_PER_MINUTE = 28;   // safety margin below ACS 30/min
    private static final int MAX_PER_HOUR   = 95;   // safety margin below ACS 100/hour
    private static final Duration LOCK_TTL  = Duration.ofSeconds(120);

    private final EmailOutboxRepository outboxRepository;
    private final EmailClient emailClient;
    private final EmailDrainLockService lockService;

    public EmailQueueDrainer(
            EmailOutboxRepository outboxRepository,
            @Qualifier("userEmailClient") EmailClient emailClient,
            EmailDrainLockService lockService) {
        this.outboxRepository = outboxRepository;
        this.emailClient = emailClient;
        this.lockService = lockService;
    }

    @Scheduled(fixedDelay = 5_000)
    public void drain() {
        if (!lockService.tryAcquireLock(LOCK_TTL)) return;

        try {
            int batchSize = calculateAvailableBatch();
            if (batchSize <= 0) return;

            List<EmailOutbox> pending = outboxRepository
                    .findPendingBatch(PageRequest.of(0, batchSize));
            if (pending.isEmpty()) return;

            for (EmailOutbox email : pending) {
                sendSingle(email);
            }

            log.debug("Email drain: sent {} of {} pending", pending.size(), batchSize);
        } finally {
            lockService.releaseLock();
        }
    }

    private void sendSingle(EmailOutbox email) {
        try {
            emailClient.sendEmail(email.getToAddress(), email.getSubject(), email.getBody());
            outboxRepository.markSent(email.getId(), Instant.now());
        } catch (Exception e) {
            log.warn("Email drain: failed to send id={} (attempt {}): {}",
                    email.getId(), email.getRetryCount() + 1, e.getMessage());
            outboxRepository.incrementRetryOrFail(email.getId());
        }
    }

    private int calculateAvailableBatch() {
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant oneHourAgo   = Instant.now().minus(1, ChronoUnit.HOURS);

        int sentLastMinute = outboxRepository.countSentSince(oneMinuteAgo);
        int sentLastHour   = outboxRepository.countSentSince(oneHourAgo);

        return Math.max(0,
                Math.min(MAX_PER_MINUTE - sentLastMinute,
                         MAX_PER_HOUR   - sentLastHour));
    }
}
