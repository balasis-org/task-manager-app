package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.exception.notfound.GroupNotFoundException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.core.validation.GroupValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.email.EmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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
    private final EmailClient emailClient;
    private final BlobStorageService blobStorageService;

    @Override
    public Group create(Group group){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(()->new UserNotFoundException("User not found"));
        groupValidator.validate(group);
        group.setOwner(user);
        Group savedGroup = groupRepository.save(group);
        groupMembershipRepository.save(new GroupMembership(user,savedGroup, Role.GROUP_LEADER));
        return savedGroup;
    }

    @Override
    public Set<Group> findAllByCurrentUser() {
        Long userId = effectiveCurrentUser.getUserId();
        return groupMembershipRepository.findByUserIdWithGroup(userId)
                .stream()
                .map(GroupMembership::getGroup)
                .collect(Collectors.toSet());
    }


    @Override
    public Group patch(Long groupId, Group group) {
        groupValidator.validateForPatch(groupId, group);
        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(()->new GroupNotFoundException("Group with id:"+groupId +" doesn't exist"));

        if (group.getName() != null) existingGroup.setName(group.getName());
        if (group.getDescription() != null) existingGroup.setDescription(group.getDescription());

        return groupRepository.save(existingGroup);
    }

    @Override
    public void delete(Long groupId) {
        taskRepository.deleteAllByGroup_Id(groupId);
        groupMembershipRepository.deleteAllByGroup_Id(groupId);
        groupRepository.deleteById(groupId);
    }




    @Override
    public Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files) {

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
            try {
                for (MultipartFile file : files){
                    System.out.println("Loop Of " + file.getOriginalFilename());
                    String url = blobStorageService.upload(file);
                    var taskFile = TaskFile.builder()
                            .fileUrl(url)
                            .name(file.getOriginalFilename())
                            .task(savedTask)
                            .build();
                    savedTask.getFiles().add(taskFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        taskRepository.save(savedTask);
//        emailClient.sendEmail("giovani1994a@gmail.com","testSub","the body message");
        //ToDO: email development to be added later. Message unified for all roles but one per id
        // included, email message example to be added:
        // New task created: “Fix invoice bug”
        // You were added to this task.
        // [Open task link]
        var thefetchedOne = taskRepository.findByIdWithParticipantsAndFiles(savedTask.getId());
        return thefetchedOne;
    }

    @Override
    public Task patchTask(Long groupId, Long taskId, Task task) {
        groupValidator.validateForPatchTask(groupId, taskId, task);
        var fetchedTask = taskRepository.findByIdWithParticipantsAndFiles(taskId);
        if (task.getTitle()!= null)
            fetchedTask.setTitle(task.getTitle());
        if (task.getDescription()!= null)
            fetchedTask.setDescription(task.getDescription());
        if (task.getTaskState()!= null)
            fetchedTask.setTaskState(task.getTaskState());
        return taskRepository.save(fetchedTask);
    }


    public String getModelName() {
        return "Group";
    }
}
