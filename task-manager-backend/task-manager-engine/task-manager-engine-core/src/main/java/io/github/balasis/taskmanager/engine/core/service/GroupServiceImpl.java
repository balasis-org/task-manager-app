package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskNotFoundException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.TaskRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.CurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.EffectiveCurrentUser;
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
    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final EmailClient emailClient;
    private final TaskRepository taskRepository;
    private final BlobStorageService blobStorageService;

    public Group create(Group group){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(()->new UserNotFoundException("User not found"));
        Group savedGroup = groupRepository.save(group);
        groupMembershipRepository.save(new GroupMembership(user,savedGroup, Role.GROUP_LEADER));
        return savedGroup;
    }

    @Override
    public List<Group> findAllByCurrentUser() {
        Long userId = effectiveCurrentUser.getUserId();
        return groupMembershipRepository.findByUserId(userId)
                .stream()
                .map(GroupMembership::getGroup)
                .toList();
    }


//    public Task create(final Task item) {
////        emailClient.sendEmail("giovani1994a@gmail.com","testSub","the body message");
//        return getRepository().save(item);
//    }
//
//    public Task createWithFile(final Task item, MultipartFile file){
//        try {
//            String url = blobStorageService.upload(file);
//            item.setFileUrl(url);
//            return getRepository().save(item);
//        } catch (IOException e) {
//            throw new TaskManagerException("Failed to upload file");
//        }
//
//    }
//
//    public Task get(final Long id) {
//        return getRepository().findById(id)
//                .orElseThrow(() -> new TaskNotFoundException(
//                        getModelName() + " with ID " + id + " not found."));
//    }
//
//
//    public void update(final Task item) {
//        if (!getRepository().existsById(item.getId())) {
//            throw new TaskNotFoundException(
//                    getModelName() + " with ID " + item.getId() + " not found.");
//        }
//        getRepository().save(item);
//    }
//
//    public void delete(final Long id) {
//        if (!getRepository().existsById(id)) {
//            throw new TaskNotFoundException(
//                    getModelName() + " with ID " + id + " not found.");
//        }
//        getRepository().deleteById(id);
//    }
//
//    @Override
//    public List<Task> findAll() {
//        return getRepository().findAll();
//    }
//
//    public boolean exists(final Task item) {
//        return getRepository().existsById(item.getId());
//    }

    public String getModelName() {
        return "Group";
    }
}
