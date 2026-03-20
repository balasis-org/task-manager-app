package io.github.balasis.taskmanager.engine.core.test.integration;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// integration tests for the GroupMembership junction table — the link between
// users and groups. covers role-based queries, JOIN FETCH loading, search with
// partial name matching, unique constraint enforcement, bulk deletes, and
// cross-group isolation (membership counts don't bleed across groups).
@DataJpaTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = AutowireMode.ALL)
class GroupMembershipIntegrationTest {

    private final GroupMembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    GroupMembershipIntegrationTest(GroupMembershipRepository membershipRepository,
                                   GroupRepository groupRepository,
                                   UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    private User leader;
    private User member;
    private Group group;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();

        leader = userRepository.save(User.builder()
                .azureKey("azure-leader")
                .email("leader@test.com")
                .name("Leader")
                .subscriptionPlan(SubscriptionPlan.ORGANIZER)
                .build());

        member = userRepository.save(User.builder()
                .azureKey("azure-member")
                .email("member@test.com")
                .name("Member")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build());

        group = groupRepository.save(Group.builder()
                .name("Test Group")
                .description("Integration test group")
                .owner(leader)
                .build());

        membershipRepository.save(GroupMembership.builder()
                .user(leader)
                .group(group)
                .role(Role.GROUP_LEADER)
                .build());

        membershipRepository.save(GroupMembership.builder()
                .user(member)
                .group(group)
                .role(Role.MEMBER)
                .build());
    }

    @Test
    void findByUserIdAndGroupId_returnsCorrectMembership() {
        Optional<GroupMembership> gm =
                membershipRepository.findByUserIdAndGroupId(leader.getId(), group.getId());
        assertTrue(gm.isPresent());
        assertEquals(Role.GROUP_LEADER, gm.get().getRole());
    }

    @Test
    void findByUserIdWithGroup_fetchJoinsOwner() {
        List<GroupMembership> memberships =
                membershipRepository.findByUserIdWithGroup(member.getId());
        assertEquals(1, memberships.size());
        assertEquals("Test Group", memberships.get(0).getGroup().getName());
        assertEquals("Leader", memberships.get(0).getGroup().getOwner().getName());
    }

    @Test
    void existsByGroupIdAndUserId_correctForBothUsers() {
        assertTrue(membershipRepository.existsByGroupIdAndUserId(group.getId(), leader.getId()));
        assertTrue(membershipRepository.existsByGroupIdAndUserId(group.getId(), member.getId()));

        User outsider = userRepository.save(User.builder()
                .azureKey("azure-outsider")
                .email("outsider@test.com")
                .name("Outsider")
                .subscriptionPlan(SubscriptionPlan.FREE)
                .build());
        assertFalse(membershipRepository.existsByGroupIdAndUserId(group.getId(), outsider.getId()));
    }

    @Test
    void countByGroup_Id_returnsCorrectCount() {
        assertEquals(2, membershipRepository.countByGroup_Id(group.getId()));
    }

    @Test
    void countByUser_Id_returnsCorrectCount() {
        assertEquals(1, membershipRepository.countByUser_Id(leader.getId()));
        assertEquals(1, membershipRepository.countByUser_Id(member.getId()));
    }

    @Test
    void findByGroup_IdAndRole_findsGroupLeader() {
        Optional<GroupMembership> leaderMembership =
                membershipRepository.findByGroup_IdAndRole(group.getId(), Role.GROUP_LEADER);
        assertTrue(leaderMembership.isPresent());
        assertEquals(leader.getId(), leaderMembership.get().getUser().getId());
    }

    @Test
    void searchByGroupIdAndUser_filtersCorrectly() {
        Page<GroupMembership> results = membershipRepository
                .searchByGroupIdAndUser(group.getId(), "Lead", PageRequest.of(0, 10));
        assertEquals(1, results.getTotalElements());
        assertEquals("Leader", results.getContent().get(0).getUser().getName());
    }

    @Test
    void searchByGroupIdAndUser_nullQuery_returnsAll() {
        Page<GroupMembership> results = membershipRepository
                .searchByGroupIdAndUser(group.getId(), null, PageRequest.of(0, 10));
        assertEquals(2, results.getTotalElements());
    }

    @Test
    void deleteByGroupIdAndUserId_removesOnlyTargeted() {
        membershipRepository.deleteByGroupIdAndUserId(group.getId(), member.getId());
        membershipRepository.flush();

        assertFalse(membershipRepository.existsByGroupIdAndUserId(group.getId(), member.getId()));
        assertTrue(membershipRepository.existsByGroupIdAndUserId(group.getId(), leader.getId()));
        assertEquals(1, membershipRepository.countByGroup_Id(group.getId()));
    }

    @Test
    void uniqueConstraint_samePair_throwsException() {
        assertThrows(Exception.class, () -> {
            membershipRepository.saveAndFlush(GroupMembership.builder()
                    .user(leader)
                    .group(group)
                    .role(Role.MEMBER)
                    .build());
        });
    }

    @Test
    void findByIdWithUser_eagerLoadsUser() {
        GroupMembership saved = membershipRepository
                .findByUserIdAndGroupId(member.getId(), group.getId()).orElseThrow();

        Optional<GroupMembership> result = membershipRepository.findByIdWithUser(saved.getId());
        assertTrue(result.isPresent());
        assertEquals("member@test.com", result.get().getUser().getEmail());
    }

    @Test
    void deleteAllByGroup_Id_removesAllMembershipsForGroup() {
        membershipRepository.deleteAllByGroup_Id(group.getId());
        membershipRepository.flush();

        assertEquals(0, membershipRepository.countByGroup_Id(group.getId()));
    }

    @Test
    void multipleGroups_membershipCountsAreIsolated() {
        Group secondGroup = groupRepository.save(Group.builder()
                .name("Second Group")
                .owner(leader)
                .build());

        membershipRepository.save(GroupMembership.builder()
                .user(leader)
                .group(secondGroup)
                .role(Role.GROUP_LEADER)
                .build());

        assertEquals(2, membershipRepository.countByGroup_Id(group.getId()));
        assertEquals(1, membershipRepository.countByGroup_Id(secondGroup.getId()));
        assertEquals(2, membershipRepository.countByUser_Id(leader.getId()));
    }
}
