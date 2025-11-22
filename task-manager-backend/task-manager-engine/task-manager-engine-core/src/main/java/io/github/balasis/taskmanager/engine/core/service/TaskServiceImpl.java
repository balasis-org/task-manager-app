package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final BlobStorageService blobStorageService;
    private final EmailClient emailClient;

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

    public Task get(final Long id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new TaskNotFoundException(
                        getModelName() + " with ID " + id + " not found."));
    }


    public void update(final Task item) {
        if (!getRepository().existsById(item.getId())) {
            throw new TaskNotFoundException(
                    getModelName() + " with ID " + item.getId() + " not found.");
        }
        getRepository().save(item);
    }

    public void delete(final Long id) {
        if (!getRepository().existsById(id)) {
            throw new TaskNotFoundException(
                    getModelName() + " with ID " + id + " not found.");
        }
        getRepository().deleteById(id);
    }

    @Override
    public List<Task> findAll() {
        return getRepository().findAll();
    }

    public boolean exists(final Task item) {
        return getRepository().existsById(item.getId());
    }

    public String getModelName(){
        return "Task";
    }

}
