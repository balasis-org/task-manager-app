package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BasicServiceImpl;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends BasicServiceImpl<Task, TaskNotFoundException> implements TaskService {
    private final TaskRepository taskRepository;
    private final BlobStorageService blobStorageService;
    private final EmailClient emailClient;

    @Override
    public JpaRepository<Task, Long> getRepository() {
        return taskRepository;
    }

    @Override
    public Task create(final Task item) {
//        emailClient.sendEmail("giovani1994a@gmail.com","testSub","the body message");
        return getRepository().save(item);
    }

    public Task createWithFile(final Task item, MultipartFile file){
        try {
            String url = blobStorageService.upload(file);
            item.setFileUrl(url);
            return getRepository().save(item);
        } catch (IOException e) {
            throw new TaskManagerException("Failed to upload file");
        }

    }

    @Override
    public Class<TaskNotFoundException> getNotFoundExceptionClass() {
        return TaskNotFoundException.class;
    }

    @Override
    public String getModelName() {
        return "task";
    }
}
