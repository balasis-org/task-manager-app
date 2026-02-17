package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DeletedTasks" , uniqueConstraints = @UniqueConstraint(columnNames = {"group_id","deletedTaskId"}))
public class DeletedTask extends BaseModel{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column
    private Long deletedTaskId;
}
