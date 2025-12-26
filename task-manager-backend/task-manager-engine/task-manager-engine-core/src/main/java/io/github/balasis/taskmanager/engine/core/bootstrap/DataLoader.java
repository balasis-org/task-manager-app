package io.github.balasis.taskmanager.engine.core.bootstrap;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"DataLoader"})
@RequiredArgsConstructor //TODO:make it run after defaultImageBootstrap
public class DataLoader extends BaseComponent implements ApplicationRunner {

    private static final Lorem lorem = LoremIpsum.getInstance();
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final UserContext userContext;
    private final DefaultImageService defaultImageService;

    @Override
    public void run(ApplicationArguments args) {
        logger.trace("=== Running DataLoader ===");
        User admin = seedInitialUser();
        userContext.setUserId(admin.getId());
        try {
            seedGroups();
        } finally {
            userContext.clear();
        }

        logger.trace("=== DataLoader finished ===");
    }

    private User seedInitialUser() {
        logger.trace("Seeding base user...");

        String email = "admin@example.com";

        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .azureKey("local-seed-user")
                                .email(email)
                                .isOrg(false)
                                .defaultImgUrl(defaultImageService.pickRandom(BlobContainerType.PROFILE_IMAGES))
                                .name("System Seeder")
                                .build()
                ));
    }

    private void seedGroups() {
        logger.trace("Seeding groups...");

        for (int i = 0; i < 2; i++) {
            try {
                createGroup();
            } catch (Exception ex) {
                logger.warn("Group seed error: {}", ex.getMessage());
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
