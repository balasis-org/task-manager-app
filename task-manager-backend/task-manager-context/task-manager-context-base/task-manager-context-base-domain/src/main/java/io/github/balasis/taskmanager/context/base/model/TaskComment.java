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
@Table(name = "TaskComments")
public class TaskComment extends BaseModel{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Task task;

    @ManyToOne
    @JoinColumn
    private User creator;

    @Lob
    @Column
    private String comment;

    @Column
    private Instant createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }

}
