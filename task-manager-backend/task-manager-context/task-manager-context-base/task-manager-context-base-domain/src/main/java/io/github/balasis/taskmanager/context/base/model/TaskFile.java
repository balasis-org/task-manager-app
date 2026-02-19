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
@Table(name = "TaskFiles", indexes = {
        @Index(name = "idx_tf_task", columnList = "task_id")
})
public class TaskFile extends BaseModel{
    @Column(length = 500)
    private String fileUrl;

    @Column(length = 255)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
}
