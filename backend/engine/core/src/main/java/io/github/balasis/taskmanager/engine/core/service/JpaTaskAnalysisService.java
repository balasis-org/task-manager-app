package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import io.github.balasis.taskmanager.context.base.model.TaskAnalysisRequest;
import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;
import io.github.balasis.taskmanager.engine.core.repository.TaskAnalysisRequestRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskAnalysisSnapshotRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskCommentRepository;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.TaskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// core-layer impl of TaskAnalysisService. handles the lifecycle before the drainer picks it up:
// estimate credits (including inter-region egress cost), check for in-progress requests,
// enqueue a new analysis, and retrieve the snapshot.
// credit formula: 3 credits/comment for analysis, ceil(totalChars/5120) for summarization,
// plus a small egress surcharge for Italy North ↔ West Europe cross-region traffic.
@Service
@Profile({"prod-h2", "prod-azuresql", "prod-arena-security", "prod-arena-stress", "dev-h2", "dev-mssql", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class JpaTaskAnalysisService implements TaskAnalysisService {

    private final TaskAnalysisRequestRepository requestRepository;
    private final TaskAnalysisSnapshotRepository snapshotRepository;
    private final TaskCommentRepository commentRepository;

    @Override
    @Transactional
    public TaskAnalysisSnapshot estimateCredits(Long taskId) {
        List<Object[]> result = commentRepository.countAndSumCharsByTaskId(taskId);
        Object[] stats = result.isEmpty() ? new Object[]{0L, 0L} : result.get(0);
        int commentCount = ((Number) stats[0]).intValue();
        long totalChars = ((Number) stats[1]).longValue();

        int analysisCredits = commentCount * 3;
        // abstractive summarization costs 2× extractive on Azure ($4 vs $2 per 1k records),
        // so we double the credit charge to pass the real cost through to the user.
        int summaryCredits = commentCount == 0
                ? 0
                : Math.max(1, (int) Math.ceil((double) totalChars / 5120)) * 2;

        // Inter-region egress: Italy North ↔ West Europe (€0.02/GB).
        // Round-trip ≈ totalChars × 2; 1 credit covers ~48.9 MB of transfer.
        // Floor of 1 credit per analysis ensures every request bills egress.
        int egressCredits = commentCount == 0
                ? 0
                : Math.max(1, (int) Math.ceil((totalChars * 2.0) / 48_900_000));

        TaskAnalysisSnapshot snapshot = snapshotRepository.findByTaskId(taskId)
                .orElse(TaskAnalysisSnapshot.builder().taskId(taskId).build());

        snapshot.setEstimatedCommentCount(commentCount);
        snapshot.setEstimatedTotalChars(totalChars);
        snapshot.setEstimatedAnalysisCredits(analysisCredits);
        snapshot.setEstimatedSummaryCredits(summaryCredits);
        snapshot.setEstimatedEgressCredits(egressCredits);
        snapshot.setEstimatedAt(Instant.now());

        return snapshotRepository.save(snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveRequest(Long taskId) {
        return requestRepository.existsByTaskIdAndStatusIn(taskId,
                List.of("PENDING", "PROCESSING"));
    }

    @Override
    @Transactional
    public void enqueueAnalysis(Long taskId, Long groupId, Long userId,
                                AnalysisType type, int credits) {
        requestRepository.save(TaskAnalysisRequest.builder()
                .taskId(taskId)
                .groupId(groupId)
                .requestedByUserId(userId)
                .analysisType(type)
                .creditsCharged(credits)
                .status("PENDING")
                .retryCount(0)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAnalysisSnapshot getSnapshot(Long taskId) {
        return snapshotRepository.findByTaskId(taskId).orElse(null);
    }
}
