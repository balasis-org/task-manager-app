package io.github.balasis.taskmanager.context.base.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TaskFilesDeleted")
public class TaskFileDeleted extends BaseModel{
    private String fileUrl;
}
