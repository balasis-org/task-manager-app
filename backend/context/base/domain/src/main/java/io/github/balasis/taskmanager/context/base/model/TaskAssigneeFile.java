package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// same structure as TaskFile but for files uploaded by the assignee.
// lives in a separate blob container ("task-assignee-files") and has its own
// count limit per task. keeping them apart makes the review workflow cleaner
// because reviewers primarily care about what the assignee submitted.
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskAssigneeFiles", indexes = {
        @Index(name = "idx_taf_task", columnList = "task_id")
})
public class TaskAssigneeFile extends BaseModel {
    @Column(length = 500)
    private String fileUrl;

    @Column(length = 255)
    private String name;

    @Column
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploadedById")
    private User uploadedBy;

    @OneToMany(mappedBy = "taskAssigneeFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FileReviewStatus> fileReviewStatuses = new HashSet<>();

    @Column
    private Instant createdAt;

    @PrePersist
    void defaults() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
