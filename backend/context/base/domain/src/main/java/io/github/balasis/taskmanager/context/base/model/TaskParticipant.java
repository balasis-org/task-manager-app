package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// links a user to a task with a specific role (CREATOR, ASSIGNEE, or REVIEWER).
// the unique constraint on (user, task, role) means a user can actually have
// multiple roles on the same task (e.g. be both CREATOR and REVIEWER) but
// cant be assigned the same role twice.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskParticipants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","task_id","task_participant_role"}),
        indexes = @Index(name = "idx_tp_task", columnList = "task_id")
)
public class TaskParticipant extends BaseModel{
    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name ="user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name ="task_id")
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_participant_role",nullable = false)
    private TaskParticipantRole taskParticipantRole;

    // tracks when this participant last viewed the comments thread.
    // compared against task.lastCommentDate to show "new comments" badge.
    @Column
    private Instant lastSeenTaskComments;

    @PrePersist
    protected void onCreate(){
        lastSeenTaskComments = Instant.now();
    }

}
