package io.github.balasis.taskmanager.engine.core.service;


import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BaseService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GroupService extends BaseService{
    Group create(Group group);
    List<Group> findAllByCurrentUser();
    Task createTask(Long groupId, Task task, Long assignedId, Long reviewerId, MultipartFile file);

//    Task getTask(Long groupID,final Long id);
//    void updateTask(Long groupID,final Task item);
//    void deleteTask(Long groupID,final Long id);
//    List<Task> findAllTasks();

}
