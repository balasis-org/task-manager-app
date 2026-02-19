package io.github.balasis.taskmanager.engine.core.test;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import io.github.balasis.taskmanager.engine.core.service.GroupServiceImpl;
import io.github.balasis.taskmanager.engine.core.service.authorization.AuthorizationService;
import io.github.balasis.taskmanager.engine.core.validation.GroupValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.mockito.Mockito.*;

class GroupServiceTest {

    private GroupRepository groupRepository;
    private GroupValidator groupValidator;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private TaskCommentRepository taskCommentRepository;
    private TaskParticipantRepository taskParticipantRepository;
    private TaskFileRepository taskFileRepository;
    private TaskAssigneeFileRepository taskAssigneeFileRepository;
    private GroupMembershipRepository groupMembershipRepository;
    private GroupEventRepository groupEventRepository;
    private EffectiveCurrentUser effectiveCurrentUser;
    private ObjectProvider<EmailClient> emailClientProvider;
    private BlobStorageService blobStorageService;
    private GroupServiceImpl groupService;
    private AuthorizationService authorizationService;
    private GroupInvitationRepository groupInvitationRepository;
    private DeletedTaskRepository deletedTaskRepository;
    private DefaultImageService defaultImageService;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        groupValidator = mock(GroupValidator.class);
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        taskCommentRepository = mock(TaskCommentRepository.class);
        taskParticipantRepository = mock(TaskParticipantRepository.class);

        taskFileRepository = mock(TaskFileRepository.class);
        taskAssigneeFileRepository = mock(TaskAssigneeFileRepository.class);
        groupMembershipRepository = mock(GroupMembershipRepository.class);
        groupEventRepository = mock (GroupEventRepository.class);
        effectiveCurrentUser = mock(EffectiveCurrentUser.class);
        deletedTaskRepository = mock(DeletedTaskRepository.class);

        emailClientProvider = mock(ObjectProvider.class);
        blobStorageService = mock(BlobStorageService.class);
        authorizationService = mock(AuthorizationService.class);
        defaultImageService = mock(DefaultImageService.class);
        groupInvitationRepository= mock(GroupInvitationRepository.class);
        groupService = new GroupServiceImpl(
                groupRepository,
                groupValidator,
                userRepository,
                taskRepository,
            taskCommentRepository,
                taskParticipantRepository,
                taskFileRepository,
                taskAssigneeFileRepository,
                groupMembershipRepository,
                groupEventRepository,
                effectiveCurrentUser,
                emailClientProvider,
                blobStorageService,
                authorizationService,
                defaultImageService,
                groupInvitationRepository,
                deletedTaskRepository

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
