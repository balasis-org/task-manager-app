package io.github.balasis.taskmanager.engine.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import io.github.balasis.taskmanager.context.base.enumeration.OverallSentiment;
import io.github.balasis.taskmanager.context.base.model.TaskAnalysisRequest;
import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;
import io.github.balasis.taskmanager.context.base.model.TaskComment;
import io.github.balasis.taskmanager.engine.core.repository.TaskAnalysisRequestRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskAnalysisSnapshotRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskCommentRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.redis.CommentAnalysisLockService;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.BatchAnalysisResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentDocument;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.CommentSummaryResult;
import io.github.balasis.taskmanager.engine.infrastructure.textanalytics.TextAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// drains the TaskAnalysisRequest queue every 10s behind a Redis distributed lock.
//
// uses CAS (Compare-And-Swap) via casUpdateStatus(id, "PENDING", "PROCESSING") to
// atomically flip the request status. if another replica already flipped it, the
// UPDATE returns 0 rows affected and we skip it — prevents double-processing.
//
// runs 3 Azure AI Language APIs per request (bundled in one beginAnalyzeActions call):
//   - Sentiment analysis: positive/negative/neutral per comment + overall
//   - Key phrase extraction: top-20 meaningful noun phrases across all comments
//   - PII detection: counts personally identifiable information entities
//     (PII = Personally Identifiable Information: SSNs, emails, phone numbers, etc.)
// optionally runs extractive summarization (AI picks representative sentences).
//
// on failure: retries up to 3x, then marks FAILED and refunds the user's analysis
// credits (credits are the tier-based quota for running AI analysis).
@Service
@Profile({"prod-h2", "prod-azuresql", "prod-arena-security", "prod-arena-stress", "dev-h2", "dev-mssql", "dev-flyway-mssql"})
public class CommentAnalysisDrainer {

    private static final Logger logger = LoggerFactory.getLogger(CommentAnalysisDrainer.class);

    private static final int BATCH_SIZE = 5;
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 3;

    private final TaskAnalysisRequestRepository requestRepository;
    private final TaskAnalysisSnapshotRepository snapshotRepository;
    private final TaskCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TextAnalyticsService textAnalyticsService;
    private final CommentAnalysisLockService lockService;
    private final ObjectMapper objectMapper;
    private final AiServiceHealthTracker aiHealthTracker;
    private final TransactionTemplate txTemplate;

    public CommentAnalysisDrainer(
            TaskAnalysisRequestRepository requestRepository,
            TaskAnalysisSnapshotRepository snapshotRepository,
            TaskCommentRepository commentRepository,
            UserRepository userRepository,
            TextAnalyticsService textAnalyticsService,
            CommentAnalysisLockService lockService,
            ObjectMapper objectMapper,
            AiServiceHealthTracker aiHealthTracker,
            PlatformTransactionManager txManager) {
        this.requestRepository = requestRepository;
        this.snapshotRepository = snapshotRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.textAnalyticsService = textAnalyticsService;
        this.lockService = lockService;
        this.objectMapper = objectMapper;
        this.aiHealthTracker = aiHealthTracker;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedDelay = 10_000)
    public void drain() {
        if (!lockService.tryAcquireLock(LOCK_TTL)) return;
        try {
            List<TaskAnalysisRequest> batch = requestRepository.findByStatus(
                    "PENDING", PageRequest.of(0, BATCH_SIZE));
            for (TaskAnalysisRequest request : batch) {
                processRequest(request);
            }
        } finally {
            lockService.releaseLock();
        }
    }

    private void processRequest(TaskAnalysisRequest request) {
        // Phase 1 — claim request and load comments (short DB transaction)
        record AnalysisInput(List<CommentDocument> docs, Long taskId,
                             AnalysisType type, int commentCount) {}
        AnalysisInput input;
        try {
            input = txTemplate.execute(status -> {
                int updated = requestRepository.casUpdateStatus(
                        request.getId(), "PENDING", "PROCESSING");
                if (updated == 0) return null;

                List<TaskComment> comments = commentRepository.findAllByTask_id(
                        request.getTaskId(), Pageable.unpaged()).getContent();

                if (comments.isEmpty()) {
                    completeRequest(request);
                    return null;
                }

                return new AnalysisInput(
                        comments.stream()
                                .map(c -> new CommentDocument(c.getId().toString(), c.getComment()))
                                .toList(),
                        request.getTaskId(),
                        request.getAnalysisType(),
                        comments.size());
            });
        } catch (Exception e) {
            logger.warn("Phase 1 failed for request id={}: {}", request.getId(), e.getMessage());
            handleFailure(request);
            return;
        }
        if (input == null) return;

        // Phase 2 — call Azure Text Analytics (no DB connection held)
        BatchAnalysisResult analysisResult = null;
        CommentSummaryResult summaryResult = null;
        try {
            if (input.type() == AnalysisType.ANALYSIS_ONLY || input.type() == AnalysisType.FULL) {
                analysisResult = textAnalyticsService.analyzeBatch(input.docs());
                aiHealthTracker.markTextAnalyticsHealthy();
            }
            if (input.type() == AnalysisType.SUMMARY_ONLY || input.type() == AnalysisType.FULL) {
                summaryResult = textAnalyticsService.summarizeBatch(input.docs());
                aiHealthTracker.markTextAnalyticsHealthy();
            }
        } catch (Exception e) {
            logger.warn("Azure API failed for request id={}: {}", request.getId(), e.getMessage());
            handleFailure(request);
            return;
        }

        // Phase 3 — persist results (short DB transaction)
        final BatchAnalysisResult ar = analysisResult;
        final CommentSummaryResult sr = summaryResult;
        try {
            txTemplate.executeWithoutResult(status -> {
                TaskAnalysisSnapshot snapshot = snapshotRepository.findByTaskId(input.taskId())
                        .orElse(TaskAnalysisSnapshot.builder().taskId(input.taskId()).build());

                Instant now = Instant.now();

                if (ar != null) {
                    snapshot.setOverallSentiment(OverallSentiment.valueOf(ar.overallSentiment()));
                    snapshot.setOverallConfidence(ar.overallConfidence());
                    snapshot.setPositiveCount(ar.positiveCount());
                    snapshot.setNeutralCount(ar.neutralCount());
                    snapshot.setNegativeCount(ar.negativeCount());
                    snapshot.setKeyPhrases(toJson(ar.topKeyPhrases()));
                    snapshot.setPiiDetectedCount(ar.totalPiiCount());
                    snapshot.setCommentResults(toJson(ar.commentResults()));
                    snapshot.setAnalysisCommentCount(input.commentCount());
                    snapshot.setAnalyzedAt(now);
                    snapshot.setAnalysisChangeMarker(now);
                }

                if (sr != null) {
                    snapshot.setSummaryText(sr.summaryText());
                    snapshot.setSummaryCommentCount(sr.commentCount());
                    snapshot.setSummarizedAt(now);
                    snapshot.setSummaryChangeMarker(now);
                }

                snapshotRepository.save(snapshot);
                completeRequest(request);
            });
        } catch (Exception e) {
            logger.warn("Phase 3 failed for request id={}: {}", request.getId(), e.getMessage());
            handleFailure(request);
        }
    }

    private void completeRequest(TaskAnalysisRequest request) {
        request.setStatus("COMPLETED");
        request.setProcessedAt(Instant.now());
        requestRepository.save(request);
    }

    // bump retry count; if maxed out, mark FAILED and refund credits back to the user
    private void handleFailure(TaskAnalysisRequest request) {
        txTemplate.executeWithoutResult(status -> {
            request.setRetryCount(request.getRetryCount() + 1);
            if (request.getRetryCount() >= MAX_RETRIES) {
                request.setStatus("FAILED");
                request.setProcessedAt(Instant.now());
                userRepository.decrementTaskAnalysisCredits(
                        request.getRequestedByUserId(), request.getCreditsCharged());
                aiHealthTracker.markTextAnalyticsDegraded();
            } else {
                request.setStatus("PENDING");
            }
            requestRepository.save(request);
        });
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.warn("JSON serialization failed: {}", e.getMessage());
            return "[]";
        }
    }
}
