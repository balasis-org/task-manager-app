package io.github.balasis.taskmanager.engine.infrastructure.email;

/**
 * Enqueues an outbound email for rate-limited delivery.
 * Production and dev implementations persist to the EmailOutbox SQL table;
 * arena profiles use a no-op implementation.
 */
public interface EmailQueueService {

    void enqueue(String to, String subject, String body);
}
