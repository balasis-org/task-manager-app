package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.FileReviewDecision;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// per-reviewer status for a file. a single file can have multiple reviews from
// different reviewers. points to either a TaskFile or a TaskAssigneeFile (one will
// be null and the other set). this is intentional, keeps the schema simple vs having
// a polymorphic FK or an intermediate table.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "FileReviewStatuses")
public class FileReviewStatus extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskFileId")
    private TaskFile taskFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taskAssigneeFileId")
    private TaskAssigneeFile taskAssigneeFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewerId", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileReviewDecision status;

    @Column(length = 200)
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void defaults() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
