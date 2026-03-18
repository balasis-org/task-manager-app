package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.TaskAnalysisRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// queue for AI task analysis requests (TEAMS_PRO feature). the drainer picks
// PENDING entries, calls Azure AI, and writes results to TaskAnalysisSnapshot.
// casUpdateStatus does an optimistic compare-and-swap to prevent two drainer
// instances from processing the same request.
public interface TaskAnalysisRequestRepository extends JpaRepository<TaskAnalysisRequest, Long> {

    List<TaskAnalysisRequest> findByStatus(String status, Pageable pageable);

    boolean existsByTaskIdAndStatusIn(Long taskId, List<String> statuses);

    @Modifying
    @Query("""
        UPDATE TaskAnalysisRequest r
        SET r.status = :newStatus
        WHERE r.id = :id
          AND r.status = :expectedStatus
    """)
    int casUpdateStatus(@Param("id") Long id,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("newStatus") String newStatus);
}
