package io.github.balasis.taskmanager.context.web.resource;

import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.validation.custom.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskResource extends BaseResource{
    @NotBlank(message = "title is mandatory")
    private String title;

    @NotBlank(message = "description is mandatory")
    private String description;

    @NotNull(message = "Task state is mandatory ")
    @ValidEnum(enumClass = TaskState.class, message = "Invalid task state")
    private String taskState;

    private String fileUrl;
}
