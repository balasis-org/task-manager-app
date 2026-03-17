package io.github.balasis.taskmanager.engine.infrastructure.email;

// Enqueues an outbound email for rate-limited delivery.
// Prod and dev impls persist to the EmailOutbox table; arena profiles use a no-op.
public interface EmailQueueService {

    void enqueue(String to, String subject, String body);
}
