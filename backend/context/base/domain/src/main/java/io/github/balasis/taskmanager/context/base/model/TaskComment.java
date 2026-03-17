package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TaskComments", indexes = {
        @Index(name = "idx_tc_task",    columnList = "task_id"),
        @Index(name = "idx_tc_creator", columnList = "creator_id")
})
public class TaskComment extends BaseModel{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Task task;

    // nullable because if the user gets deleted we keep the comment but lose the FK.
    // thats why we also store creatorNameSnapshot.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private User creator;

    // preserved so comments still show a name even after the user is gone
    @Column
    private String creatorNameSnapshot;

    // nvarchar because comments can contain unicode (emoji etc)
    @Column(columnDefinition = "nvarchar(800)")
    private String comment;

    @Column
    private Instant createdAt;

    // set by the AI analysis pipeline when PII is detected in the comment text.
    // the frontend can use this to show a warning badge.
    @Column
    @lombok.Builder.Default
    private boolean containsPii = false;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }

}
