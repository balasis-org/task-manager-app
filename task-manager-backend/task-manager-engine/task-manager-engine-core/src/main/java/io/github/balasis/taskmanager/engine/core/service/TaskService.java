package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BaseService;
import org.springframework.web.multipart.MultipartFile;

public interface TaskService extends BaseService<Task,Long> {
    Task createWithFile(Task task, MultipartFile file);
}
