package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.engine.core.service.TaskService;
import io.github.balasis.taskmanager.engine.core.validation.TaskValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.Random;

@SpringBootTest
@ActiveProfiles("dev-h2")
public class TaskServiceTest {
    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskValidator taskValidator;

    @Test
    void createSampleTask() {
        Task task = Task.builder()
                .title("Test Task")
                .description("This task is created by CI test")
                .taskState(TaskState.values()[new Random().nextInt(TaskState.values().length)])
                .build();

        taskService.create(taskValidator.validate(task));

        System.out.println("Task created with ID: " + task.getId());
    }
}
