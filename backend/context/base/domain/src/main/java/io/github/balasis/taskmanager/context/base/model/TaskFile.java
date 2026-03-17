package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// a file uploaded by the task creator (or a task manager). stored in the
// "task-files" blob container. fileUrl is the blob key, not a full URL.
// the actual download goes through BlobStorageService which streams it
// from Azure Blob Storage (or Azurite in dev).
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskFiles", indexes = {
        @Index(name = "idx_tf_task", columnList = "task_id")
})
public class TaskFile extends BaseModel{
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

    // each file can have review statuses from multiple reviewers
    // (e.g. reviewer A says CHECKED, reviewer B says NEEDS_REVISION)
    @OneToMany(mappedBy = "taskFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FileReviewStatus> fileReviewStatuses = new HashSet<>();

    @Column
    private Instant createdAt;

    @PrePersist
    void defaults() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
