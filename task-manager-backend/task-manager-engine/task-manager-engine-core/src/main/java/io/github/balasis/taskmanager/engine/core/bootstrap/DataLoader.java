package io.github.balasis.taskmanager.engine.core.bootstrap;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.UserContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"DataLoader"})
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final Lorem lorem = LoremIpsum.getInstance();
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final UserContext userContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running DataLoader ===");
        User admin = seedInitialUser();
        userContext.setUserId(admin.getId());
        try {
            seedGroups();
        } finally {
            userContext.clear(); // safety
        }

        log.info("=== DataLoader finished ===");
    }

    private User seedInitialUser() {
        log.info("Seeding base user...");

        String email = "admin@example.com";

        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .azureKey("local-seed-user")
                                .email(email)
                                .isOrg(false)
                                .name("System Seeder")
                                .build()
                ));
    }

    private void seedGroups() {
        log.info("Seeding groups...");

        for (int i = 0; i < 2; i++) {
            try {
                createGroup();
            } catch (Exception ex) {
                log.warn("Group seed error: {}", ex.getMessage());
            }
        }
    }

    public void createGroup() {
        groupService.create(
                Group.builder()
                        .name(lorem.getTitle(1))
                        .description(lorem.getTitle(1))
                        .build()
        );
    }
}
