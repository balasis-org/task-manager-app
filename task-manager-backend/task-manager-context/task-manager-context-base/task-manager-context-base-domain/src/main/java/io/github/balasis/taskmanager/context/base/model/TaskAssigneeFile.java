package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskAssigneeFiles", indexes = {
        @Index(name = "idx_taf_task", columnList = "task_id")
})
public class TaskAssigneeFile extends BaseModel {
    @Column(length = 500)
    private String fileUrl;

    @Column(length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
}
