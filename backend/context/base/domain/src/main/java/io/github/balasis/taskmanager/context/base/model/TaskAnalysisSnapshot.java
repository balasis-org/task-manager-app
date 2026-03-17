package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.OverallSentiment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "TaskAnalysisSnapshots", indexes = {
        @Index(name = "idx_tas_task", columnList = "taskId", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAnalysisSnapshot extends BaseModel {

    @Column(nullable = false, unique = true)
    private Long taskId;

    // ── Estimate cache ──────────────────────────────────────────

    @Column
    private int estimatedCommentCount;

    @Column
    private long estimatedTotalChars;

    @Column
    private int estimatedAnalysisCredits;

    @Column
    private int estimatedSummaryCredits;

    @Column
    private int estimatedEgressCredits;

    @Column
    private Instant estimatedAt;

    @Column
    private Instant estimateChangeMarker;

    //Analysis results (nullable until analysis run)

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OverallSentiment overallSentiment;

    @Column
    private Double overallConfidence;

    @Column
    private Integer positiveCount;

    @Column
    private Integer neutralCount;

    @Column
    private Integer negativeCount;

    @Column(columnDefinition = "nvarchar(max)")
    private String keyPhrases;

    @Column
    private Integer piiDetectedCount;

    @Column(columnDefinition = "nvarchar(max)")
    private String commentResults;

    @Column
    private Integer analysisCommentCount;

    @Column
    private Instant analyzedAt;

    @Column
    private Instant analysisChangeMarker;

    // Summary results (nullable until summary run)

    @Column(columnDefinition = "nvarchar(max)")
    private String summaryText;

    @Column
    private Integer summaryCommentCount;

    @Column
    private Instant summarizedAt;

    @Column
    private Instant summaryChangeMarker;
}
