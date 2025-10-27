package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TaskValidatorImpl implements TaskValidator{
    private final TaskRepository taskRepository;

    @Override
    public Task validate(Task task) {
        //placeholder for exception
        return task;
    }

    @Override
    public Task validateForUpdate(Long id, Task task) {
        //placeholder for exception
        return task;
    }
}
