package io.github.balasis.taskmanager.context.base.model;


import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskParticipants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id","task_id","task_participant_role"})
})
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

    @Column
    private Instant lastSeenTaskComments;

    @PrePersist
    protected void onCreate(){
        lastSeenTaskComments = Instant.now();
    }

}
