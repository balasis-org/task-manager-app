package io.github.balasis.taskmanager.engine.core.service;


import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BaseService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface GroupService extends BaseService{
    Group create(Group group);

    Group patch(Long groupId, Group group);

    void delete(Long groupId);

    Set<Group> findAllByCurrentUser();

    Task createTask(Long groupId, Task task, Set<Long> assignedIds, Set<Long> reviewerIds, Set<MultipartFile> files);

    Task patchTask(Long groupId ,Long taskId, Task task);

    Task getTask(Long groupId, Long taskId);

    Set<Task> findMyTasks(Long groupId, Boolean reviewer, Boolean assigned, TaskState taskState);

    Task addAssignee(Long groupId, Long taskId, Long userId);

    void removeAssignee(Long groupId, Long taskId, Long userId);

    Task addReviewer(Long groupId, Long taskId, Long userId);

    void removeReviewer(Long groupId, Long taskId, Long userId);

    Task addTaskFile(Long groupId, Long taskId, MultipartFile file);

    void removeTaskFile(Long groupId, Long taskId, Long fileId);
}
