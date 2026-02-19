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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    private User creator;

    @Column
    private String creatorNameSnapshot;

    @Column(columnDefinition = "nvarchar(800)")
    private String comment;

    @Column
    private Instant createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }

}
