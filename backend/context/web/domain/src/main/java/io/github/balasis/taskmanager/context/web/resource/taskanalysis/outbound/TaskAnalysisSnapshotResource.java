package io.github.balasis.taskmanager.context.web.resource.taskanalysis.outbound;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskAnalysisSnapshotResource implements Serializable {

    // ── Analysis section (nullable until analysis run) ──────────
    private String overallSentiment;
    private Double overallConfidence;
    private Integer positiveCount;
    private Integer neutralCount;
    private Integer negativeCount;
    private List<String> keyPhrases;
    private Integer piiDetectedCount;
    private List<CommentAnalysisView> commentResults;
    private Integer analysisCommentCount;
    private Instant analyzedAt;
    private boolean analysisStale;

    // ── Summary section (nullable until summary run) ────────────
    private String summaryText;
    private Integer summaryCommentCount;
    private Instant summarizedAt;
    private boolean summaryStale;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentAnalysisView implements Serializable {
        private String commentId;
        private String sentiment;
        private double confidence;
        private List<String> keyPhrases;
        private int piiEntityCount;
    }
}
