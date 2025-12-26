package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.exception.blob.download.BlobDownloadTaskFileException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadTaskFileException;
import io.github.balasis.taskmanager.context.base.exception.notfound.*;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.core.service.authorization.AuthorizationService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import io.github.balasis.taskmanager.engine.core.validation.GroupValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class GroupServiceImpl extends BaseComponent implements GroupService{
    private final GroupRepository groupRepository;
    private final GroupValidator groupValidator;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;
//    private final EmailClient emailClient;
    private final BlobStorageService blobStorageService;
    private final AuthorizationService authorizationService;
    private final DefaultImageService defaultImageService;
    private final GroupInvitationRepository groupInvitationRepository;


    @Override
    public Group create(Group group){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(()->new UserNotFoundException("User not found"));
        groupValidator.validate(group);
        group.setOwner(user);
        group.setDefaultImgUrl(defaultImageService.pickRandom(BlobContainerType.GROUP_IMAGES));
        Group savedGroup = groupRepository.save(group);
        groupMembershipRepository.save(new GroupMembership(user,savedGroup, Role.GROUP_LEADER));
        return savedGroup;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Group> findAllByCurrentUser() {
        Long userId = effectiveCurrentUser.getUserId();
        return groupMembershipRepository.findByUserIdWithGroup(userId)
                .stream()
                .map(GroupMembership::getGroup)
                .collect(Collectors.toSet());
    }


    @Override
    public Group patch(Long groupId, Group group) {
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER));
        groupValidator.validateForPatch(groupId, group);
        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(()->new GroupNotFoundException("Group with id:"+groupId +" doesn't exist"));

        if (group.getName() != null) existingGroup.setName(group.getName());
        if (group.getDescription() != null) existingGroup.setDescription(group.getDescription());

        return groupRepository.save(existingGroup);
    }

    @Override
    public void delete(Long groupId) {
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER));
        taskRepository.deleteAllByGroup_Id(groupId);
        groupMembershipRepository.deleteAllByGroup_Id(groupId);
        groupRepository.deleteById(groupId);
    }



    @Override
    public Group updateGroupImage(Long groupId, MultipartFile file) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with id " + groupId + " not found"));

        String blobName = blobStorageService.uploadGroupImage(file, groupId);
        group.setImgUrl(blobName);

        return groupRepository.save(group);
    }


    @Override
    public GroupInvitation createGroupInvitation(Long groupId, Long userToBeInvited){
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var groupInvitation = GroupInvitation.builder()
                .user(userRepository.getReferenceById(userToBeInvited))
                .invitationStatus(InvitationStatus.PENDING)
                .invitedBy(userRepository.getReferenceById(effectiveCurrentUser.getUserId()))
                .group(groupRepository.findById(groupId).orElseThrow())
                .build();
        groupValidator.validateCreateGroupInvitation(groupInvitation);

        return groupInvitationRepository.save(groupInvitation);
    }

    @Override
    public GroupInvitation acceptInvitation(Long groupInvitationId) {
        var groupInvitation = groupInvitationRepository.findByIdWithGroup(groupInvitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        groupValidator.validateAcceptGroupInvitation(groupInvitation);
        groupMembershipRepository.save(
                new GroupMembership(
                        groupInvitation.getUser(),
                        groupInvitation.getGroup(),
                        Role.MEMBER
                )
        );
        groupInvitation.setInvitationStatus(InvitationStatus.ACCEPTED);
        return groupInvitationRepository.save(groupInvitation);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GroupInvitation> findMyGroupInvitations() {
        return groupInvitationRepository.findByUser_Id(effectiveCurrentUser.getUserId());
    }


    @Override
    public Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        groupValidator.validateForCreateTask(groupId, assignedIds, reviewerIds);

        task.setCreator(userRepository.getReferenceById(effectiveCurrentUser.getUserId()));
        task.setGroup(groupRepository.getReferenceById(groupId));

        var savedTask =  taskRepository.save(task);

        if (assignedIds != null && !assignedIds.isEmpty()){
            for(Long assignedId:assignedIds){

                var taskParticipant = TaskParticipant.builder()
                        .taskParticipantRole(TaskParticipantRole.ASSIGNEE)
                        .user(userRepository.getReferenceById(assignedId))
                        .task(savedTask)
                        .build();
                savedTask.getTaskParticipants().add(taskParticipant);
            }
        }

        if (reviewerIds != null && !reviewerIds.isEmpty()){
            for(Long reviewerId:reviewerIds){
                var taskParticipant = TaskParticipant.builder()
                        .taskParticipantRole(TaskParticipantRole.REVIEWER)
                        .user(userRepository.getReferenceById(reviewerId))
                        .task(savedTask)
                        .build();
                savedTask.getTaskParticipants().add(taskParticipant);
            }
        }

        if (files != null && !files.isEmpty()) {

                for (MultipartFile file : files){
                    System.out.println("Loop Of " + file.getOriginalFilename());
                    String url = blobStorageService.uploadTaskFile(file, savedTask.getId());
                    var taskFile = TaskFile.builder()
                            .fileUrl(url)
                            .name(file.getOriginalFilename())
                            .task(savedTask)
                            .build();
                    savedTask.getFiles().add(taskFile);
                }
        }
        taskRepository.save(savedTask);
//        emailClient.sendEmail("giovani1994a@gmail.com","testSub","the body message");
        //ToDO: email development to be added later. Message unified for all roles but one per id
        // included, email message example to be added:
        // New task created: “Fix invoice bug”
        // You were added to this task.
        // [Open task link]
        var thefetchedOne = taskRepository.findByIdWithFullFetchParticipantsAndFiles(savedTask.getId()).orElseThrow(
                () -> new TaskNotFoundException("Something went wrong with the create process of task." +
                "If the problem insist during creation conduct us and provide one of the tasks ID. Current taskId:"
                                                + savedTask.getId())
        );
        return thefetchedOne;
    }



    @Override
    @Transactional(readOnly = true)
    public Task getTask(Long groupId, Long taskId){
        authorizationService.requireAnyRoleIn(groupId);
        return taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId).orElseThrow(()-> new TaskNotFoundException(
                "Task with id " + taskId + "is not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Task> findMyTasks(Long groupId, Boolean reviewer, Boolean assigned, TaskState taskState) {
        return taskRepository.searchBy(groupId, effectiveCurrentUser.getUserId(), reviewer, assigned, taskState);
    }


    @Override
    public Task patchTask(Long groupId, Long taskId, Task task) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        groupValidator.validateForPatchTask(groupId, taskId, task);
        var fetchedTask = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + "is not found"));
        if (task.getTitle()!= null)
            fetchedTask.setTitle(task.getTitle());
        if (task.getDescription()!= null)
            fetchedTask.setDescription(task.getDescription());
        if (task.getTaskState()!= null)
            fetchedTask.setTaskState(task.getTaskState());
        return taskRepository.save(fetchedTask);
    }



    @Override
    public Task addAssignee(Long groupId, Long taskId, Long userId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateAddAssigneeToTask(task, groupId, userId);
        task.getTaskParticipants().add(TaskParticipant.builder()
                .task(task)
                .user(userRepository.getReferenceById(userId))
                .taskParticipantRole(TaskParticipantRole.ASSIGNEE)
                .build());
        return taskRepository.save(task);
    }

    @Override
    public Task addReviewer(Long groupId, Long taskId, Long userId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateAddReviewerToTask(task, groupId, userId);
        task.getTaskParticipants().add(TaskParticipant.builder()
                .task(task)
                .user(userRepository.getReferenceById(userId))
                .taskParticipantRole(TaskParticipantRole.REVIEWER)
                .build());
        return taskRepository.save(task);
    }

    @Override
    public Task addTaskFile(Long groupId, Long taskId, MultipartFile file) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateAddTaskFile(task, groupId, file);
        String url = blobStorageService.uploadTaskFile(file,taskId);
        task.getFiles().add(TaskFile.builder()
                .task(task)
                .name(file.getOriginalFilename())
                .fileUrl(url)
                .build());
        return taskRepository.save(task);

    }

    @Transactional(readOnly = true)
    public TaskFileDownload downloadTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireAnyRoleIn(groupId);

        var task = taskRepository.findByIdWithParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));

        groupValidator.validateDownloadTaskFile(task, groupId);

        TaskFile file = task.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new TaskFileNotFoundException("File not found"));
        byte[] data = blobStorageService.downloadTaskFile(file.getFileUrl());
        return new TaskFileDownload(data, file.getName());

    }



    @Override
    public void removeAssignee(Long groupId, Long taskId, Long userId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateRemoveAssigneeFromTask(task, groupId, userId);
        task.getTaskParticipants().removeIf(tp ->
                tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE &&
                        tp.getUser().getId().equals(userId));
        taskRepository.save(task);
    }

    @Override
    public void removeReviewer(Long groupId, Long taskId, Long userId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFullFetchParticipantsAndFiles(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateRemoveReviewerFromTask(task, groupId, userId);
        task.getTaskParticipants().removeIf(tp ->
                tp.getTaskParticipantRole() == TaskParticipantRole.REVIEWER &&
                        tp.getUser().getId().equals(userId));
        taskRepository.save(task);
    }

    @Override
    public void removeTaskFile(Long groupId, Long taskId, Long fileId) {
        authorizationService.requireRoleIn(groupId, Set.of(Role.GROUP_LEADER, Role.TASK_MANAGER));
        var task = taskRepository.findByIdWithFilesAndGroup(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " is not found"));
        groupValidator.validateRemoveTaskFile(task, groupId, fileId);

        TaskFile file = task.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow();

        blobStorageService.deleteTaskFile(file.getFileUrl());
        task.getFiles().remove(file);
        taskFileRepository.delete(file);
    }

}
