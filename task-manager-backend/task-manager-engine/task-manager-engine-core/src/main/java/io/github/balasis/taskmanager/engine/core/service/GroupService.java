package io.github.balasis.taskmanager.engine.core.service;


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
//    Task getTask(Long groupID,final Long id);
//    void updateTask(Long groupID,final Task item);
//    void deleteTask(Long groupID,final Long id);
//    List<Task> findAllTasks();

}
