package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskAnalysisRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

// queue for AI task analysis requests (TEAMS_PRO feature). the drainer picks
// PENDING entries, calls Azure AI, and writes results to TaskAnalysisSnapshot.
// casUpdateStatus does an optimistic compare-and-swap to prevent two drainer
// instances from processing the same request.
public interface TaskAnalysisRequestRepository extends JpaRepository<TaskAnalysisRequest, Long> {

    // oldest-first so no request starves — must match IX_TAR_status_createdAt composite index
    @Query("SELECT r FROM TaskAnalysisRequest r WHERE r.status = :status ORDER BY r.createdAt")
    List<TaskAnalysisRequest> findByStatus(@Param("status") String status, Pageable pageable);

    boolean existsByTaskIdAndStatusIn(Long taskId, List<String> statuses);

    @Modifying
    @Query("""
        UPDATE TaskAnalysisRequest r
        SET r.status = :newStatus, r.processedAt = :now
        WHERE r.id = :id
          AND r.status = :expectedStatus
    """)
    int casUpdateStatus(@Param("id") Long id,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("newStatus") String newStatus,
                        @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE TaskAnalysisRequest r
        SET r.status = 'PENDING'
        WHERE r.status = 'PROCESSING'
          AND r.processedAt < :threshold
    """)
    int recoverStaleProcessing(@Param("threshold") Instant threshold);
}
