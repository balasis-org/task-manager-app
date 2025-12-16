package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.exception.notfound.GroupNotFoundException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.model.TaskFile;
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


@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class GroupServiceImpl implements GroupService{
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
    public List<Group> findAllByCurrentUser() {
        Long userId = effectiveCurrentUser.getUserId();
        return groupMembershipRepository.findByUserIdWithGroup(userId)
                .stream()
                .map(GroupMembership::getGroup)
                .toList();
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
    public Task createTask(Long groupId, Task task, Long assignedId, Long reviewerId,List<MultipartFile> files) {
        groupValidator.validateForCreateTask(groupId, assignedId, reviewerId);

        task.setGroup(groupRepository.getReferenceById(groupId));
        task.setCreator(userRepository.getReferenceById(effectiveCurrentUser.getUserId()));

        if (assignedId != null)
            task.setAssigned(userRepository.getReferenceById(assignedId));

        if (reviewerId != null)
            task.setReviewer(userRepository.getReferenceById(reviewerId));

        var savedTask =  taskRepository.save(task);

        if (files != null && !files.isEmpty()) {
            try {
                for (MultipartFile file : files){
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
//        emailClient.sendEmail("giovani1994a@gmail.com","testSub","the body message");
        return taskRepository.save(task);
    }

    public String getModelName() {
        return "Group";
    }
}
