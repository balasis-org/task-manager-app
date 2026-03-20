package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.OverallSentiment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// holds the cached AI analysis results for a task's comments.
// one-to-one with taskId (unique index). split into three sections:
// 1) estimate cache: computed locally before sending to Azure, so the user can
// see how many credits it will cost before committing
// 2) analysis results: sentiment, key phrases, PII counts, per-comment breakdowns
// 3) summary results: extractive summary of the conversation
// each section is nullable until that operation has actually run.
// the "changeMarker" timestamps let the frontend know if the analysis is stale
// (more comments added since last run).
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

    // Estimate cache
    // these are computed locally (no Azure call) so we can show the user
    // a cost preview before they run the actual analysis
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

    // json blob of per-comment sentiment/keyphrase results. stored as nvarchar(max)
    // because SQL Server doesnt have a native JSON type and we just need to pass it
    // through to the frontend.
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
