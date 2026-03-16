package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

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
