package io.github.balasis.taskmanager.engine.core.bootstrap;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.service.TaskService;
import io.github.balasis.taskmanager.engine.core.validation.TaskValidator;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Component
@AllArgsConstructor
@Profile({"h2"})
public class DataLoader implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private static final Lorem lorem = LoremIpsum.getInstance();
    private final TaskService guestService;
    private final TaskValidator guestValidator;

    @Override
    public void run(ApplicationArguments args) {
        loadTasks();
    }
    private void loadTasks() {
        logger.trace("Loading tasks...");
        for (int i = 0; i < 5; i++) {
            try {
                createTasksTransactional();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }

        logger.trace("Finished loading guests");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTasksTransactional() {
        guestService.create(
                guestValidator.validate(
                        Task.builder()
                                .title(lorem.getTitle(1))
                                .description(lorem.getParagraphs(1,1))
                                .taskState(TaskState.values()[new Random().nextInt(TaskState.values().length)])
                                .build()
                )
        );
    }

}
