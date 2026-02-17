package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Tasks")
public class Task extends BaseModel{
    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @Lob
    @Column(nullable = false, length = 1500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskState taskState;

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

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private Set<TaskFile> creatorFiles = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskAssigneeFile> assigneeFiles = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL , orphanRemoval = true)
    @Builder.Default
    private Set<TaskComment> taskComments = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column
    private ReviewersDecision reviewersDecision;

    @ManyToOne
    @JoinColumn
    private User reviewedBy;

    @Lob
    @Column(length = 400)
    private String reviewComment;

    @ManyToOne
    @JoinColumn
    private User lastEditBy;

    @Column
    private Instant lastEditDate;

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

    @Column
    private Long commentCount;

    @Column
    private Integer priority;

    @Column
    private Instant lastCommentDate;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
        if (commentCount == null) {
            commentCount = 0L;
        }
    }
}
