package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// queue entry for the CommentAnalysisDrainer. when a user clicks "analyze" in the UI
// we insert a request here (inside the same transaction that charges their credits).
// the drainer picks up PENDING rows every 10 seconds in batches of 5,
// calls Azure Text Analytics, and writes results to TaskAnalysisSnapshot.
@Entity
@Table(name = "TaskAnalysisRequests", indexes = {
        @Index(name = "idx_tar_status", columnList = "status"),
        @Index(name = "idx_tar_task",   columnList = "taskId")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAnalysisRequest extends BaseModel {

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisType analysisType;

    // credits are charged upfront when the request is created.
    // if the analysis fails the credits stay spent (no refund, to prevent abuse)
    @Column(nullable = false)
    private int creditsCharged;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant processedAt;

    @PrePersist
    void defaults() {
        if (status == null) status = "PENDING";
        if (createdAt == null) createdAt = Instant.now();
    }
}
