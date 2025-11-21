package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.engine.core.service.TaskServiceImpl;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.core.validation.TaskValidator;

import io.github.balasis.taskmanager.engine.core.validation.TaskValidatorImpl;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private BlobStorageService blobStorageService;
    private EmailClient emailClient;
    private TaskValidator taskValidator;
    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        blobStorageService = mock(BlobStorageService.class);
        emailClient = mock(EmailClient.class);
        taskValidator = new TaskValidatorImpl(taskRepository);

        taskService = new TaskServiceImpl(taskRepository, blobStorageService,emailClient);
    }

    @Test
    void createSampleTask() {
        Task task = Task.builder()
                .title("Test Task")
                .description("This task is created by CI test")
                .taskState(TaskState.values()[new Random().nextInt(TaskState.values().length)])
                .build();

        Task validatedTask = taskValidator.validate(task);
        taskService.create(validatedTask);

        verify(taskRepository, times(1)).save(validatedTask);

        System.out.println("Task created with ID: " + task.getId());
    }
}