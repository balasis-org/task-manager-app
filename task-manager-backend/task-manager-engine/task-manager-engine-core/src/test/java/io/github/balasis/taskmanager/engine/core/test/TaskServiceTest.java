package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.GroupServiceImpl;
import io.github.balasis.taskmanager.engine.core.validation.GroupValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class GroupServiceTest {

    private GroupRepository groupRepository;
    private GroupValidator groupValidator;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private GroupMembershipRepository groupMembershipRepository;
    private EffectiveCurrentUser effectiveCurrentUser;
    private EmailClient emailClient;
    private BlobStorageService blobStorageService;
    private GroupServiceImpl groupService;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        groupValidator = mock(GroupValidator.class);
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        groupMembershipRepository = mock(GroupMembershipRepository.class);
        effectiveCurrentUser = mock(EffectiveCurrentUser.class);
        emailClient = mock(EmailClient.class);
        blobStorageService = mock(BlobStorageService.class);

        groupService = new GroupServiceImpl(
                groupRepository,
                groupValidator,
                userRepository,
                taskRepository,
                groupMembershipRepository,
                effectiveCurrentUser,
                emailClient,
                blobStorageService
        );
    }

    @Test
    void createGroup_success() {
        User user = User.builder().id(1L).name("Seeder").email("admin@example.com").build();
        Group group = Group.builder().name("Test Group").description("CI created").build();
        Group savedGroup = Group.builder().id(100L).name("Test Group").description("CI created").build();

        when(effectiveCurrentUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupRepository.save(group)).thenReturn(savedGroup);

        groupService.create(group);

        // verify repository calls
        verify(groupRepository, times(1)).save(group);
        verify(groupMembershipRepository, times(1))
                .save(argThat(membership ->
                        membership.getUser().equals(user) &&
                                membership.getGroup().equals(savedGroup) &&
                                membership.getRole() == Role.GROUP_LEADER
                ));
    }
}
