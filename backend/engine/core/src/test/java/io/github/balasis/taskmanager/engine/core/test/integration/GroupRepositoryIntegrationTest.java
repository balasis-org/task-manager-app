package io.github.balasis.taskmanager.engine.core.test.integration;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// integration tests for group CRUD, JOIN FETCH queries, unique constraints,
// and cascade deletes against an in-memory H2 database.
//
// em.flush() + em.clear() appears before some queries to force Hibernate
// to write pending changes to the database and then drop its first-level cache,
// so the next query hits the actual SQL rather than returning a stale object.
// this is especially important before cascade-delete tests and after saving
// related entities through a different repository.
@DataJpaTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = AutowireMode.ALL)
class GroupRepositoryIntegrationTest {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EntityManager em;

    GroupRepositoryIntegrationTest(GroupRepository groupRepository,
                                   UserRepository userRepository,
                                   GroupMembershipRepository groupMembershipRepository,
                                   EntityManager em) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.em = em;
    }

    private User owner;

    @BeforeEach
    void setUp() {
        groupMembershipRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .azureKey("azure-owner-key")
                .email("owner@test.com")
                .name("Owner")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build());
    }

    @Test
    void save_and_findById_roundTrip() {
        Group group = groupRepository.save(Group.builder()
                .name("Test Group")
                .description("A test group")
                .owner(owner)
                .build());

        Optional<Group> found = groupRepository.findById(group.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Group", found.get().getName());
        assertEquals("A test group", found.get().getDescription());
    }

    @Test
    void existsByNameAndOwner_Id_returnsTrue_whenDuplicate() {
        groupRepository.save(Group.builder()
                .name("UniqueGroup")
                .owner(owner)
                .build());

        assertTrue(groupRepository.existsByNameAndOwner_Id("UniqueGroup", owner.getId()));
        assertFalse(groupRepository.existsByNameAndOwner_Id("OtherName", owner.getId()));
    }

    @Test
    void countByOwner_Id_countsCorrectly() {
        groupRepository.save(Group.builder().name("Group A").owner(owner).build());
        groupRepository.save(Group.builder().name("Group B").owner(owner).build());

        assertEquals(2, groupRepository.countByOwner_Id(owner.getId()));
    }

    @Test
    void findByIdWithTasksAndParticipants_eagerFetchesOwner() {
        Group group = groupRepository.save(Group.builder()
                .name("Fetch Group")
                .description("Test JOIN FETCH")
                .owner(owner)
                .build());

        Optional<Group> result = groupRepository.findByIdWithTasksAndParticipants(group.getId());
        assertTrue(result.isPresent());
        assertEquals(owner.getId(), result.get().getOwner().getId());
        assertEquals("Owner", result.get().getOwner().getName());
    }

    @Test
    void adminFindByIdWithDetails_loadsOwnerAndMemberships() {
        Group group = groupRepository.save(Group.builder()
                .name("Admin Group")
                .owner(owner)
                .build());

        groupMembershipRepository.save(GroupMembership.builder()
                .user(owner)
                .group(group)
                .role(Role.GROUP_LEADER)
                .build());

        em.flush();
        em.clear();

        Optional<Group> result = groupRepository.adminFindByIdWithDetails(group.getId());
        assertTrue(result.isPresent());
        assertFalse(result.get().getMemberships().isEmpty());
        assertEquals(Role.GROUP_LEADER,
                result.get().getMemberships().iterator().next().getRole());
    }

    @Test
    void findByIdWithOwner_loadsOwnerEagerly() {
        Group group = groupRepository.save(Group.builder()
                .name("Owner Fetch")
                .owner(owner)
                .build());

        Optional<Group> result = groupRepository.findByIdWithOwner(group.getId());
        assertTrue(result.isPresent());
        assertEquals("owner@test.com", result.get().getOwner().getEmail());
    }

    @Test
    void uniqueConstraint_sameNameSameOwner_throwsException() {
        groupRepository.save(Group.builder()
                .name("Dup Name")
                .owner(owner)
                .build());

        assertThrows(Exception.class, () -> {
            groupRepository.saveAndFlush(Group.builder()
                    .name("Dup Name")
                    .owner(owner)
                    .build());
        });
    }

    @Test
    void differentOwners_canHaveSameGroupName() {
        User otherOwner = userRepository.save(User.builder()
                .azureKey("azure-other-key")
                .email("other@test.com")
                .name("Other")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build());

        groupRepository.save(Group.builder().name("Shared Name").owner(owner).build());
        Group second = groupRepository.save(Group.builder()
                .name("Shared Name")
                .owner(otherOwner)
                .build());

        assertNotNull(second.getId());
        assertEquals(1, groupRepository.countByOwner_Id(owner.getId()));
        assertEquals(1, groupRepository.countByOwner_Id(otherOwner.getId()));
    }

    @Test
    void deletingGroup_cascadesTo_memberships() {
        Group group = groupRepository.save(Group.builder()
                .name("Cascade Group")
                .owner(owner)
                .build());

        groupMembershipRepository.save(GroupMembership.builder()
                .user(owner)
                .group(group)
                .role(Role.GROUP_LEADER)
                .build());

        em.flush();
        em.clear();

        assertEquals(1, groupMembershipRepository.countByGroup_Id(group.getId()));

        Group managed = groupRepository.findById(group.getId()).orElseThrow();
        groupRepository.delete(managed);
        groupRepository.flush();

        assertEquals(0, groupMembershipRepository.countByGroup_Id(group.getId()));
    }
}
