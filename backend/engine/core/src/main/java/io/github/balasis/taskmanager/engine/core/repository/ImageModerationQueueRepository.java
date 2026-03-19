package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.ImageModerationQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

// queue table for async image moderation. images are scanned by a scheduled
// drainer (ImageModerationDrainer) that picks PENDING entries in batch.
// countViolationsSince powers the 4-level escalation ban logic.
@Repository
public interface ImageModerationQueueRepository extends JpaRepository<ImageModerationQueue, Long> {

    // oldest-first ordering so no image starves in the queue
    @Query("SELECT q FROM ImageModerationQueue q WHERE q.status = 'PENDING' ORDER BY q.createdAt")
    List<ImageModerationQueue> findPendingBatch(Pageable pageable);

    // counts recent violations for a user — feeds the escalation ladder:
    // 1st = warning, 2nd = 24h ban, 3rd = 7d ban, 4th+ = permanent
    @Query("""
        SELECT COUNT(q) FROM ImageModerationQueue q
        WHERE q.status = 'REJECTED'
          AND q.userId = :userId
          AND q.processedAt > :since
    """)
    int countViolationsSince(@Param("userId") Long userId, @Param("since") Instant since);

    // bump retry count and auto-fail after 3 attempts so poison entries
    // dont block the queue forever
    @Modifying
    @Query("""
        UPDATE ImageModerationQueue q
        SET q.retryCount = q.retryCount + 1,
            q.status = CASE WHEN q.retryCount >= 2 THEN 'FAILED' ELSE q.status END
        WHERE q.id = :id
    """)
    void incrementRetryOrFail(@Param("id") Long id);
}
