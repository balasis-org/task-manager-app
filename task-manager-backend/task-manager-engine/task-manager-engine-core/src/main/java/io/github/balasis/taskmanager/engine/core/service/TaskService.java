package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BaseService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskService extends BaseService {
    Task createWithFile(Task task, MultipartFile file);
    Task create(final Task item);
    Task get(final Long id);
    void update(final Task item);
    void delete(final Long id);
    List<Task> findAll();
    boolean exists(Task item);
}
