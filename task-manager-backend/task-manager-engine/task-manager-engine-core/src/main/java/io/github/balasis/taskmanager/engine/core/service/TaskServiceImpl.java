package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.exception.notfound.TaskNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BasicServiceImpl;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends BasicServiceImpl<Task, TaskNotFoundException> implements TaskService {
    private final TaskRepository taskRepository;

    @Override
    public JpaRepository<Task, Long> getRepository() {
        return taskRepository;
    }

    @Override
    public Class<TaskNotFoundException> getNotFoundExceptionClass() {
        return TaskNotFoundException.class;
    }

    @Override
    public String getModelName() {
        return "Room";
    }
}
