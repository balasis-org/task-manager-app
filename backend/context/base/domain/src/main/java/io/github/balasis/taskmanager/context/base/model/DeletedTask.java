package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// this is NOT a soft-delete table. its a tombstone for poll-based sync.
// when a task gets deleted we insert a row here so the next time the frontend
// polls refreshGroup it sees "task X was deleted" and removes it from the UI.
// without this the frontend would never know a task disappeared between polls.
// rows are purged after 30 days by DatabaseCleanupService in the maintenance job.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DeletedTasks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id","deletedTaskId"}),
        indexes = @Index(name = "idx_dt_group_deleted", columnList = "group_id, deletedAt")
)
public class DeletedTask extends BaseModel{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // the PK the task had before we deleted it, so the frontend can match
    // it against its local cache and drop it
    @Column
    private Long deletedTaskId;

    @Column(nullable = false)
    private java.time.Instant deletedAt;
}
