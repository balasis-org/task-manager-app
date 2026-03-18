package io.github.balasis.taskmanager.engine.core.bootstrap;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.ReviewersDecision;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.GroupInvitationRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.UserContext;
import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import lombok.RequiredArgsConstructor;
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

// seeds the database with fake users, groups, tasks, invitations, and near-limit
// budget counters for local dev. only active when the "DataLoader" Spring profile
// is enabled. creates one group per subscription tier so every plan can be tested.
// LOWEST_PRECEDENCE ordering ensures DefaultImageBootstrap runs first — we need
// default images available before assigning them to seed users.
@Component
@Profile({"DataLoader"})
@RequiredArgsConstructor
public class DataLoader extends BaseComponent {

    private static final Lorem lorem = LoremIpsum.getInstance();
    private static final String SEED_TENANT_ID = "dev-fake-tenant";
    private static final String GROUP_A_LEADER_AZURE_KEY = "dev-fake:lena.dev@example.com";

    private final UserRepository userRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupService groupService;
    private final UserContext userContext;
    private final DefaultImageService defaultImageService;
    private final StartupGate startupGate;

    // idempotent: if lena already exists, the whole data load is skipped.
    // the finally block marks StartupGate as data-ready even on failure,
    // otherwise the app would stay in 503 mode forever.
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onApplicationReady(ApplicationReadyEvent evt)  {
        logger.trace("=== Running DataLoader ===");

        try {
            if (userRepository.existsByAzureKey(GROUP_A_LEADER_AZURE_KEY)) {
                logger.trace("Seed users already exist; skipping DataLoader run.");
                return;
            }

            Map<String, User> users = seedUsers();
            try {
                seedFreeTierGroup(users);
                seedStudentTierGroup(users);
                seedOrganizerTierGroup(users);
                seedTeamTierGroup(users);
                seedTeamsProTierGroup(users);
            } finally {
                userContext.clear();
            }

            applyNearLimitBudgets(users);

            logger.trace("=== DataLoader finished ===");
        } finally {
            startupGate.markDataReady();
        }
    }

    // members shared across 3+ groups need at least STUDENT plan
    // because FREE only allows 2 group memberships
    private Map<String, User> seedUsers() {
        logger.trace("Seeding users...");

        Map<String, SeedUser> seeds = new LinkedHashMap<>();

        // Tier leaders — each gets a specific subscription plan
        seeds.put("LENA",   new SeedUser(devAzureKey("lena.dev@example.com"),   "lena.dev@example.com",   "Lena Dev",   SubscriptionPlan.FREE));
        seeds.put("MARCO",  new SeedUser(devAzureKey("marco.dev@example.com"),  "marco.dev@example.com",  "Marco Dev",  SubscriptionPlan.STUDENT));
        seeds.put("NINA",   new SeedUser(devAzureKey("nina.dev@example.com"),   "nina.dev@example.com",   "Nina Dev",   SubscriptionPlan.ORGANIZER));
        seeds.put("TOMAS",  new SeedUser(devAzureKey("tomas.dev@example.com"),  "tomas.dev@example.com",  "Tomas Dev",  SubscriptionPlan.TEAM));
        seeds.put("ALINA",  new SeedUser(devAzureKey("alina.dev@example.com"),  "alina.dev@example.com",  "Alina Dev",  SubscriptionPlan.TEAMS_PRO));

        // TODO: Remove admin seed user before final submission — added for diagnostics testing only
        seeds.put("ADMIN",  new SeedUser(devAzureKey("admin.dev@example.com"),  "admin.dev@example.com",  "Admin Dev",  SubscriptionPlan.TEAMS_PRO));

        // Members shared across 3+ groups need STUDENT plan (maxGroups=5)
        seeds.put("SOFIA",  new SeedUser(devAzureKey("sofia.dev@example.com"),  "sofia.dev@example.com",  "Sofia Dev",  SubscriptionPlan.STUDENT));
        seeds.put("PETER",  new SeedUser(devAzureKey("peter.dev@example.com"),  "peter.dev@example.com",  "Peter Dev",  SubscriptionPlan.STUDENT));
        seeds.put("HANNA",  new SeedUser(devAzureKey("hanna.dev@example.com"),  "hanna.dev@example.com",  "Hanna Dev",  SubscriptionPlan.STUDENT));
        seeds.put("LEON",   new SeedUser(devAzureKey("leon.dev@example.com"),   "leon.dev@example.com",   "Leon Dev",   SubscriptionPlan.STUDENT));

        // Regular members in ≤2 groups (FREE is fine)
        seeds.put("ERIK",   new SeedUser(devAzureKey("erik.dev@example.com"),   "erik.dev@example.com",   "Erik Dev",   null));
        seeds.put("JULIA",  new SeedUser(devAzureKey("julia.dev@example.com"),  "julia.dev@example.com",  "Julia Dev",  null));
        seeds.put("RAVI",   new SeedUser(devAzureKey("ravi.dev@example.com"),   "ravi.dev@example.com",   "Ravi Dev",   null));
        seeds.put("KATYA",  new SeedUser(devAzureKey("katya.dev@example.com"),  "katya.dev@example.com",  "Katya Dev",  null));

        // stress-test users (stress01 - stress38)
        for (int i = 1; i <= 38; i++) {
            String num   = String.format("%02d", i);
            String key   = "STRESS" + num;
            String email = "stress" + num + ".dev@example.com";
            String name  = "Stress" + num + " Dev";
            seeds.put(key, new SeedUser(devAzureKey(email), email, name, null));
        }

        Map<String, User> created = new LinkedHashMap<>();
        for (Map.Entry<String, SeedUser> entry : seeds.entrySet()) {
            SeedUser seed = entry.getValue();
            User user = userRepository.findByAzureKey(seed.azureKey)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .azureKey(seed.azureKey)
                                    .tenantId(SEED_TENANT_ID)
                                    .email(seed.email)
                                    .name(seed.name)
                                    .isOrg(false)
                                    .allowEmailNotification(false)
                                    .subscriptionPlan(seed.plan != null ? seed.plan : SubscriptionPlan.FREE)
                                    .defaultImgUrl(defaultImageService.pickRandom(BlobContainerType.PROFILE_IMAGES))
                                    .build()
                    ));
            created.put(entry.getKey(), user);
        }

        // TODO: Remove admin promotion before final submission — added for diagnostics testing only
        User adminUser = created.get("ADMIN");
        if (adminUser != null) {
            adminUser.setSystemRole(SystemRole.ADMIN);
            userRepository.save(adminUser);
        }

        logger.trace("Seeded {} users", created.size());
        return created;
    }

    // ── Free Tier Group (leader: Lena, cap 8) ─────────────────────
    private void seedFreeTierGroup(Map<String, User> users) {
        logger.trace("Seeding Free Tier Group...");

        User leader = users.get("LENA");

        withUser(leader, () -> {
            groupService.create(Group.builder()
                    .name("Free Tier Group")
                    .description("Free-plan group (8 member cap)")
                    .announcement("Welcome to the Free Tier Group")
                    .build());
        });
    }

    // ── Student Tier Group (leader: Marco, cap 20) ──────────────
    private void seedStudentTierGroup(Map<String, User> users) {
        logger.trace("Seeding Student Tier Group...");

        User leader = users.get("MARCO");

        withUser(leader, () -> {
            Group group = groupService.create(Group.builder()
                    .name("Student Tier Group")
                    .description("Student-plan group (20 member cap)")
                    .announcement("Welcome to the Student Tier Group")
                    .build());

            inviteAndAccept(group.getId(), users.get("ERIK"),  Role.TASK_MANAGER, "Seed invite");
            inviteAndAccept(group.getId(), users.get("JULIA"), Role.REVIEWER,     "Seed invite");
            inviteAndAccept(group.getId(), users.get("RAVI"),  Role.MEMBER,       "Seed invite");
            inviteAndAccept(group.getId(), users.get("KATYA"), Role.MEMBER,       "Seed invite");

            seedTasksForGroup(group.getId(), users.get("ERIK"), users.get("JULIA"), users.get("RAVI"), users.get("KATYA"));
        });
    }

    // ── Organizer Tier Group (leader: Nina, cap 30) ─────────────
    private void seedOrganizerTierGroup(Map<String, User> users) {
        logger.trace("Seeding Organizer Tier Group...");

        User leader = users.get("NINA");

        withUser(leader, () -> {
            Group group = groupService.create(Group.builder()
                    .name("Organizer Tier Group")
                    .description("Organizer-plan group (30 member cap)")
                    .announcement("Welcome to the Organizer Tier Group")
                    .build());

            inviteAndAccept(group.getId(), users.get("LEON"),  Role.TASK_MANAGER, "Seed invite");
            inviteAndAccept(group.getId(), users.get("SOFIA"), Role.REVIEWER,     "Seed invite");
            inviteAndAccept(group.getId(), users.get("PETER"), Role.MEMBER,       "Seed invite");
            inviteAndAccept(group.getId(), users.get("HANNA"), Role.MEMBER,       "Seed invite");

            seedTasksForGroup(group.getId(), users.get("LEON"), users.get("SOFIA"), users.get("PETER"), users.get("HANNA"));
        });
    }

    // ── Teams Pro Tier Group (leader: Alina, cap 50) ────────────────
    private void seedTeamsProTierGroup(Map<String, User> users) {
        logger.trace("Seeding Teams Pro Tier Group...");

        User leader = users.get("ALINA");

        withUser(leader, () -> {
            Group group = groupService.create(Group.builder()
                    .name("Teams Pro Tier Group")
                    .description("Teams-Pro plan group (Comment Intelligence enabled)")
                    .announcement("Welcome to the Teams Pro Tier Group")
                    .build());

            inviteAndAccept(group.getId(), users.get("SOFIA"), Role.TASK_MANAGER, "Seed invite");
            inviteAndAccept(group.getId(), users.get("PETER"), Role.REVIEWER,     "Seed invite");
            inviteAndAccept(group.getId(), users.get("HANNA"), Role.MEMBER,       "Seed invite");
            inviteAndAccept(group.getId(), users.get("LEON"),  Role.MEMBER,       "Seed invite");

            seedTasksForGroup(group.getId(), users.get("SOFIA"), users.get("PETER"), users.get("HANNA"), users.get("LEON"));
        });
    }

    // ── Team Tier Group (leader: Tomas, cap 50, stress-test target) ──
    private void seedTeamTierGroup(Map<String, User> users) {
        logger.trace("Seeding Team Tier Group (stress target)...");

        User leader = users.get("TOMAS");

        withUser(leader, () -> {
            Group group = groupService.create(Group.builder()
                    .name("Team Tier Group")
                    .description("Team-plan group used for k6 stress / presence testing")
                    .announcement("Stress testing in progress")
                    .build());

            // core members with various roles
            inviteAndAccept(group.getId(), users.get("LENA"),  Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("MARCO"), Role.TASK_MANAGER, "Stress seed");
            inviteAndAccept(group.getId(), users.get("NINA"),  Role.REVIEWER,     "Stress seed");
            inviteAndAccept(group.getId(), users.get("SOFIA"), Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("PETER"), Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("HANNA"), Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("ERIK"),  Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("JULIA"), Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("RAVI"),  Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("KATYA"), Role.MEMBER,       "Stress seed");
            inviteAndAccept(group.getId(), users.get("LEON"),  Role.MEMBER,       "Stress seed");

            // all 38 stress-test users
            for (int i = 1; i <= 38; i++) {
                String key = String.format("STRESS%02d", i);
                inviteAndAccept(group.getId(), users.get(key), Role.MEMBER, "Stress seed");
            }
        });
    }

    // withUser temporarily swaps the UserContext so GroupService sees
    // the seed user as the "logged in" user. crucial because group creation
    // assigns ownership to the current user.
    private void inviteAndAccept(Long groupId, User invitee, Role role, String comment) {
        User inviteeWithCode = userRepository.findById(invitee.getId()).orElseThrow();
        if (inviteeWithCode.getInviteCode() == null || inviteeWithCode.getInviteCode().isBlank()) {
            inviteeWithCode.refreshInviteCode();
            userRepository.save(inviteeWithCode);
        }

        groupService.createGroupInvitation(groupId, inviteeWithCode.getInviteCode(), role, comment, false);

        groupInvitationRepository
            .findTopByGroup_IdAndUser_IdAndInvitationStatusOrderByIdDesc(
                groupId,
                inviteeWithCode.getId(),
                InvitationStatus.PENDING)
            .ifPresentOrElse(
                invitation -> withUser(inviteeWithCode, () ->
                    groupService.respondToInvitation(invitation.getId(), InvitationStatus.ACCEPTED)
                ),
                () -> logger.warn("Seed invite missing for user {} in group {}", inviteeWithCode.getId(), groupId)
            );
        }

    private void seedTasksForGroup(Long groupId, User manager, User reviewer, User member1, User member2) {
        logger.trace("Seeding tasks for groupId={}...", groupId);

        withUser(manager, () -> {
            Instant dueSoon = Instant.now().plus(7, ChronoUnit.DAYS);

            Task todo1 = groupService.createTask(
                    groupId,
                    Task.builder()
                            .title(manager.getName() + " - Setup dark mode toggle")
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
                            .title(manager.getName() + " - Profile picture not saving")
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
                            .title(manager.getName() + " - Add sorting to task list")
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
                            .title(manager.getName() + " - Export group report as PDF")
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
                            .title(manager.getName() + " - Move deadline picker to modal")
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

    // ── Near-limit budget seeding ────────────────────────────────────
    // All tier leaders are fully consumed except ALINA (TEAMS_PRO),
    // who keeps a tiny gap — the only account that can still use resources.
    // sets all tier leaders to their plan's exact limits (fully consumed) except
    // ALINA (TEAMS_PRO) who keeps a tiny gap. this lets us test "over budget" UI
    // states for every tier while still having one account that can do stuff.
    private void applyNearLimitBudgets(Map<String, User> users) {
        logger.trace("Setting budget counters near plan limits...");

        // FREE — LENA: download budget fully consumed
        User lena = users.get("LENA");
        lena.setUsedDownloadBytesMonth(500L * 1024 * 1024);     // 500 MB = limit
        userRepository.save(lena);

        // STUDENT — MARCO: fully consumed
        User marco = users.get("MARCO");
        marco.setUsedImageScansMonth(50);                        // 50/50
        marco.setUsedDownloadBytesMonth(4L * 1024 * 1024 * 1024); // 4 GB = limit
        marco.setUsedStorageBytes(500L * 1024 * 1024);           // 500 MB = limit
        userRepository.save(marco);

        // ORGANIZER — NINA: fully consumed
        User nina = users.get("NINA");
        nina.setUsedImageScansMonth(100);                         // 100/100
        nina.setUsedEmailsMonth(150);                             // 150/150
        nina.setUsedDownloadBytesMonth(25L * 1024 * 1024 * 1024); // 25 GB = limit
        nina.setUsedStorageBytes(2L * 1024 * 1024 * 1024);       // 2 GB = limit
        userRepository.save(nina);

        // TEAM — TOMAS: fully consumed
        User tomas = users.get("TOMAS");
        tomas.setUsedImageScansMonth(150);                         // 150/150
        tomas.setUsedEmailsMonth(10_000);                          // 10,000/10,000
        tomas.setUsedDownloadBytesMonth(50L * 1024 * 1024 * 1024); // 50 GB = limit
        tomas.setUsedStorageBytes(5L * 1024 * 1024 * 1024);       // 5 GB = limit
        userRepository.save(tomas);

        // TEAMS_PRO — ALINA: the ONE account with remaining allowance
        User alina = users.get("ALINA");
        alina.setUsedImageScansMonth(148);                         // 2 scans remaining
        alina.setUsedEmailsMonth(9_998);                           // 2 emails remaining
        alina.setUsedTaskAnalysisCreditsMonth(7_990);              // 10 credits remaining
        alina.setUsedDownloadBytesMonth(50L * 1024 * 1024 * 1024 - 100L * 1024 * 1024); // ~100 MB remaining
        alina.setUsedStorageBytes(5L * 1024 * 1024 * 1024 - 50L * 1024 * 1024);         // ~50 MB remaining
        userRepository.save(alina);
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
        private final SubscriptionPlan plan;

        private SeedUser(String azureKey, String email, String name, SubscriptionPlan plan) {
            this.azureKey = azureKey;
            this.email = email;
            this.name = name;
            this.plan = plan;
        }
    }

    private static String devAzureKey(String email) {
        return "dev-fake:" + email;
    }

    private int randomPriority() {
        return ThreadLocalRandom.current().nextInt(0, 11);
    }

}
