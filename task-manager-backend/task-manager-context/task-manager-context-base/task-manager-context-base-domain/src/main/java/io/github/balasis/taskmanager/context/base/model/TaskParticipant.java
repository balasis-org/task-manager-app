package io.github.balasis.taskmanager.context.base.model;


import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskParticipant")
public class TaskParticipant extends BaseModel{
    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name ="user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name ="task_id")
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskParticipantRole taskParticipantRole;
}
