package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.EmailOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

// transactional outbox pattern: emails are written as PENDING rows inside
// the same DB transaction as the triggering action, then picked up by
// EmailQueueDrainer on a 10-second schedule. same retry+fail logic as image moderation.
@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Query("SELECT e FROM EmailOutbox e WHERE e.status = 'PENDING' ORDER BY e.createdAt")
    List<EmailOutbox> findPendingBatch(Pageable pageable);

    @Query("SELECT COUNT(e) FROM EmailOutbox e WHERE e.status = 'SENT' AND e.sentAt > :since")
    int countSentSince(@Param("since") Instant since);

    @Modifying
    @Query("UPDATE EmailOutbox e SET e.status = 'SENT', e.sentAt = :now WHERE e.id = :id")
    void markSent(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE EmailOutbox e SET e.retryCount = e.retryCount + 1, e.status = CASE WHEN e.retryCount >= 2 THEN 'FAILED' ELSE e.status END WHERE e.id = :id")
    void incrementRetryOrFail(@Param("id") Long id);
}
