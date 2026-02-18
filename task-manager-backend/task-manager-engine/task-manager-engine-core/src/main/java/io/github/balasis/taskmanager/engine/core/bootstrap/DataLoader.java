package io.github.balasis.taskmanager.engine.core.bootstrap;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupInvitation;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


@Component
@Profile({"DataLoader"})
@RequiredArgsConstructor
public class DataLoader extends BaseComponent {

    private static final Lorem lorem = LoremIpsum.getInstance();
    private static final String SEED_TENANT_ID = "dev-fake-tenant";
    private static final String GROUP_A_LEADER_AZURE_KEY = "dev-fake:alice.dev@example.com";

    private final UserRepository userRepository;
    private final GroupService groupService;
    private final UserContext userContext;
    private final DefaultImageService defaultImageService;
    private final StartupGate startupGate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onApplicationReady(ApplicationReadyEvent evt)  {
        logger.trace("=== Running DataLoader ===");

        seedInitialUser();

        if (userRepository.existsByAzureKey(GROUP_A_LEADER_AZURE_KEY)) {
            logger.trace("Seed users already exist; skipping DataLoader run.");
            return;
        }

        Map<String, User> users = seedUsers();
        try {
            seedGroupA(users);
            seedGroupB(users);
        } finally {
            userContext.clear();
        }

        startupGate.markDataReady();

        logger.trace("=== DataLoader finished ===");
    }

    private User seedInitialUser() {
        logger.trace("Seeding base user...");
        String email = "admin@example.com";

        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .azureKey("local-seed-user")
                                .tenantId(SEED_TENANT_ID)
                                .email(email)
                                .isOrg(false)
                                .allowEmailNotification(false)
                                .defaultImgUrl(defaultImageService.pickRandom(BlobContainerType.PROFILE_IMAGES))
                                .name("System Seeder")
                                .build()
                ));
    }

    private Map<String, User> seedUsers() {
        logger.trace("Seeding users...");

        Map<String, SeedUser> seeds = new LinkedHashMap<>();

        seeds.put("ALICE", new SeedUser(devAzureKey("alice.dev@example.com"), "alice.dev@example.com", "Alice Dev"));
        seeds.put("BOB", new SeedUser(devAzureKey("bob.dev@example.com"), "bob.dev@example.com", "Bob Dev"));
        seeds.put("CAROL", new SeedUser(devAzureKey("carol.dev@example.com"), "carol.dev@example.com", "Carol Dev"));
        seeds.put("DAVE", new SeedUser(devAzureKey("dave.dev@example.com"), "dave.dev@example.com", "Dave Dev"));
        seeds.put("ERIN", new SeedUser(devAzureKey("erin.dev@example.com"), "erin.dev@example.com", "Erin Dev"));
        seeds.put("FRANK", new SeedUser(devAzureKey("frank.dev@example.com"), "frank.dev@example.com", "Frank Dev"));
        seeds.put("GRACE", new SeedUser(devAzureKey("grace.dev@example.com"), "grace.dev@example.com", "Grace Dev"));
        seeds.put("HEIDI", new SeedUser(devAzureKey("heidi.dev@example.com"), "heidi.dev@example.com", "Heidi Dev"));
        seeds.put("IVAN", new SeedUser(devAzureKey("ivan.dev@example.com"), "ivan.dev@example.com", "Ivan Dev"));
        seeds.put("JUDY", new SeedUser(devAzureKey("judy.dev@example.com"), "judy.dev@example.com", "Judy Dev"));
        seeds.put("MALLORY", new SeedUser(devAzureKey("mallory.dev@example.com"), "mallory.dev@example.com", "Mallory Dev"));
        seeds.put("OSCAR", new SeedUser(devAzureKey("oscar.dev@example.com"), "oscar.dev@example.com", "Oscar Dev"));

        var whitelistOtherTenant = new HashSet<>(
                Set.of(
                        seeds.get("ALICE").name,
                        seeds.get("GRACE").name,
                        seeds.get("JUDY").name,
                        seeds.get("BOB").name
                )
        );

        Map<String, User> created = new LinkedHashMap<>();
        for (Map.Entry<String, SeedUser> entry : seeds.entrySet()) {
            SeedUser seed = entry.getValue();
            User user = userRepository.findByAzureKey(seed.azureKey)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .azureKey(seed.azureKey)
                                    .tenantId((whitelistOtherTenant.contains(seed.name)) ? "OtherTenant" :SEED_TENANT_ID)
                                    .email(seed.email)
                                    .name(seed.name)
                                    .isOrg(false)
                                    .allowEmailNotification(false)
                                    .defaultImgUrl(defaultImageService.pickRandom(BlobContainerType.PROFILE_IMAGES))
                                    .build()
                    ));
            created.put(entry.getKey(), user);
        }

        logger.trace("Seeded {} users", created.size());
        return created;
    }

    private void seedGroupA(Map<String, User> users) {
        logger.trace("Seeding Group A...");

        User leader = users.get("ALICE");
        User manager = users.get("BOB");
        User reviewer = users.get("CAROL");
        User member1 = users.get("DAVE");
        User member2 = users.get("ERIN");
        User guest = users.get("FRANK");

        User reviewer2 = users.get("GRACE");

        withUser(leader, () -> {
            Group group = groupService.create(Group.builder()
                    .name("Seed Group A")
                    .description("Seeded group A (for dev testing)")
                    .Announcement("Welcome to Seed Group A")
                    .build());

            inviteAndAccept(group.getId(), manager, Role.TASK_MANAGER, "Seed invite: task manager");
            inviteAndAccept(group.getId(), reviewer, Role.REVIEWER, "Seed invite: reviewer");
            inviteAndAccept(group.getId(), member1, Role.MEMBER, "Seed invite: member");
            inviteAndAccept(group.getId(), member2, Role.MEMBER, "Seed invite: member");
            inviteAndAccept(group.getId(), guest, Role.GUEST, "Seed invite: guest");

            inviteAndAccept(group.getId(), reviewer2, Role.REVIEWER, "Seed invite: reviewer");

            seedTasksForGroup(group.getId(), manager, reviewer, member1, member2);

            patchGroupForEvents(group.getId(), leader,
                    "Seeded group A (patched)",
                    "Welcome to dev Group A (patched)");
        });
    }

    private void seedGroupB(Map<String, User> users) {
        logger.trace("Seeding Group B...");

        User leader = users.get("GRACE");
        User manager = users.get("HEIDI");
        User reviewer = users.get("IVAN");
        User member1 = users.get("JUDY");
        User member2 = users.get("MALLORY");
        User guest = users.get("OSCAR");


        User reviewer2 = users.get("ALICE");

        withUser(leader, () -> {
            Group group = groupService.create(Group.builder()
                    .name("Seed Group B")
                    .description("Seeded group B (for dev testing)")
                    .Announcement("Welcome to Seed Group B")
                    .build());

            inviteAndAccept(group.getId(), manager, Role.TASK_MANAGER, "Seed invite: task manager");
            inviteAndAccept(group.getId(), reviewer, Role.REVIEWER, "Seed invite: reviewer");
            inviteAndAccept(group.getId(), member1, Role.MEMBER, "Seed invite: member");
            inviteAndAccept(group.getId(), member2, Role.MEMBER, "Seed invite: member");
            inviteAndAccept(group.getId(), guest, Role.GUEST, "Seed invite: guest");

            inviteAndAccept(group.getId(), reviewer2, Role.REVIEWER, "Seed invite: reviewer");

            seedTasksForGroup(group.getId(), manager, reviewer, member1, member2);

            patchGroupForEvents(group.getId(), leader,
                    "Seeded group B (patched)",
                    "Welcome to dev Group B (patched)");
        });
    }

    private void inviteAndAccept(Long groupId, User invitee, Role role, String comment) {
        GroupInvitation invitation = groupService.createGroupInvitation(groupId, invitee.getId(), role, comment);
        withUser(invitee, () -> groupService.respondToInvitation(invitation.getId(), InvitationStatus.ACCEPTED));
    }

    private void seedTasksForGroup(Long groupId, User manager, User reviewer, User member1, User member2) {
        logger.trace("Seeding tasks for groupId={}...", groupId);

        withUser(manager, () -> {
            Instant dueSoon = Instant.now().plus(7, ChronoUnit.DAYS);

            Task todo1 = groupService.createTask(
                    groupId,
                    Task.builder()
                            .title(manager.getName() + " - Implement session refresh")
                            .description(lorem.getParagraphs(1, 2))
                        .taskState(TaskState.TODO)
                        .priority(randomPriority())
                        .dueDate(dueSoon)
                            .build(),
                    Set.of(member1.getId()),
                    Set.of(reviewer.getId()),
                    Set.of()
            );
            groupService.addTaskComment(groupId, todo1.getId(), "Seed comment: created TODO #1");

            Task todo2 = groupService.createTask(
                    groupId,
                    Task.builder()
                            .title(manager.getName() + " - Fix avatar upload error")
                            .description(lorem.getParagraphs(1, 2))
                        .taskState(TaskState.TODO)
                        .priority(randomPriority())
                        .dueDate(dueSoon)
                            .build(),
                    Set.of(member2.getId()),
                    Set.of(reviewer.getId()),
                    Set.of()
            );
            groupService.addTaskComment(groupId, todo2.getId(), "Seed comment: created TODO #2");

            Task inProgress = groupService.createTask(
                    groupId,
                    Task.builder()
                            .title(manager.getName() + " - Integrate payment gateway")
                            .description(lorem.getParagraphs(1, 2))
                        .taskState(TaskState.IN_PROGRESS)
                        .priority(randomPriority())
                        .dueDate(dueSoon)
                            .build(),
                    Set.of(member1.getId()),
                    Set.of(reviewer.getId()),
                    Set.of()
            );
            groupService.addTaskComment(groupId, inProgress.getId(), "Seed comment: started work");

            Task toBeReviewed = groupService.createTask(
                    groupId,
                    Task.builder()
                            .title(manager.getName() + " - Bulk CSV import feature")
                            .description(lorem.getParagraphs(1, 2))
                        .taskState(TaskState.IN_PROGRESS)
                        .priority(randomPriority())
                        .dueDate(dueSoon)
                            .build(),
                    Set.of(member2.getId()),
                    Set.of(reviewer.getId()),
                    Set.of()
            );
            groupService.addTaskComment(groupId, toBeReviewed.getId(), "Seed comment: ready for review soon");
            withUser(member2, () -> {
                groupService.addTaskComment(groupId, toBeReviewed.getId(), "Seed assignee comment: marking for review");
                groupService.markTaskToBeReviewed(groupId, toBeReviewed.getId());
            });

            Task done = groupService.createTask(
                    groupId,
                    Task.builder()
                            .title(manager.getName() + " - Redesign comments UI")
                            .description(lorem.getParagraphs(1, 2))
                        .taskState(TaskState.IN_PROGRESS)
                        .priority(randomPriority())
                        .dueDate(dueSoon)
                            .build(),
                    Set.of(member1.getId()),
                    Set.of(reviewer.getId()),
                    Set.of()
            );
            withUser(member1, () -> {
                groupService.addTaskComment(groupId, done.getId(), "Seed assignee comment: implemented fix");
                groupService.markTaskToBeReviewed(groupId, done.getId());
            });
            withUser(reviewer, () -> {
                groupService.addTaskComment(groupId, done.getId(), "Seed reviewer comment: reviewing now");
                groupService.reviewTask(groupId, done.getId(), Task.builder()
                        .reviewersDecision(ReviewersDecision.APPROVE)
                        .reviewComment("Seed review: looks good, approved")
                        .build());
            });
        });
    }

    private void patchGroupForEvents(Long groupId, User asUser, String newDescription, String newAnnouncement) {
        withUser(asUser, () -> groupService.patch(groupId, Group.builder()
                .description(newDescription)
                .Announcement(newAnnouncement)
                .build()));
    }

    private void withUser(User user, Runnable action) {
        Long previousUserId = userContext.getUserId();
        userContext.setUserId(user.getId());
        try {
            action.run();
        } finally {
            if (previousUserId == null) {
                userContext.clear();
            } else {
                userContext.setUserId(previousUserId);
            }
        }
    }

    private static class SeedUser {
        private final String azureKey;
        private final String email;
        private final String name;

        private SeedUser(String azureKey, String email, String name) {
            this.azureKey = azureKey;
            this.email = email;
            this.name = name;
        }
    }

    private static String devAzureKey(String email) {
        return "dev-fake:" + email;
    }

    private int randomPriority() {
        return ThreadLocalRandom.current().nextInt(0, 11);
    }

}
