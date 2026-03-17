package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// the core work unit. belongs to exactly one group. has a state machine lifecycle:
// TODO -> IN_PROGRESS -> TO_BE_REVIEWED -> DONE (with some allowed backwards transitions).
// the group_id + title pair is unique so you cant have two tasks with the same name in one group.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Tasks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"title", "group_id"}),
        indexes = {
        @Index(name = "idx_task_group",             columnList = "group_id"),
        @Index(name = "idx_task_group_lastchange",   columnList = "group_id, lastChangeDate")
})
public class Task extends BaseModel{
    @Column(nullable = false, length = 150)
    private String title;

    @Lob
    @Column(nullable = false, length = 1500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState taskState;

    // snapshot of the creator's id and name at task creation time.
    // if the creator later leaves the group or gets deleted, we still
    // know who originally created the task without a FK that would break.
    @Column(name = "creator_id_snapshot")
    private Long creatorIdSnapshot;

    @Column(name = "creator_name_snapshot", length = 100)
    private String creatorNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskParticipant> taskParticipants = new HashSet<>();

    // two separate file collections: creator files (attached by task creator or managers)
    // and assignee files (uploaded by the person assigned to the task).
    // they live in different blob containers and have separate count limits.
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private Set<TaskFile> creatorFiles = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskAssigneeFile> assigneeFiles = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL , orphanRemoval = true)
    @Builder.Default
    private Set<TaskComment> taskComments = new HashSet<>();

    // set when a REVIEWER approves or rejects the task in TO_BE_REVIEWED state
    @Enumerated(EnumType.STRING)
    @Column
    private ReviewersDecision reviewersDecision;

    @ManyToOne
    @JoinColumn
    private User reviewedBy;

    @Lob
    @Column(length = 400)
    private String reviewComment;

    // tracks who last edited and when (shown in the UI as "last edited by X")
    @ManyToOne
    @JoinColumn
    private User lastEditBy;

    @Column
    private Instant lastEditDate;

    // multiple change timestamps because the frontend polls at different granularities.
    // lastChangeDate = any change at all (used by group-level refresh)
    // lastChangeDateNoJoins = only direct task field changes (not file/participant/comment changes)
    // lastChangeDateInParticipants, lastChangeDateInComments = scoped to those child collections
    // the frontend compares these against its cached values to know what to re-fetch
    @Column
    private Instant lastChangeDate;

    @Column
    private Instant lastChangeDateNoJoins;

    @Column
    private Instant lastChangeDateInParticipants;

    @Column
    private Instant lastChangeDateInComments;

    @Column
    private Instant createdAt;

    @Column
    private Instant dueDate;

    // denormalized comment count so we can show it in task previews without
    // loading the entire comments collection
    @Column
    private Long commentCount;

    @Column
    private Integer priority;

    @Column
    private Instant lastCommentDate;

    // task-level overrides for file limits. when set these take priority
    // over group overrides and plan defaults. see Group for the cascade logic.
    @Column
    private Integer maxCreatorFiles;

    @Column
    private Integer maxAssigneeFiles;

    @Column
    private Long maxFileSizeBytes;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
        if (commentCount == null) {
            commentCount = 0L;
        }
    }
}
