package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.Task;

import java.util.Set;

public interface GroupValidator extends BaseValidator<Group>{
    void validateForCreateTask(Long groupId, Set<Long> assignedIds, Set<Long> reviewerIds);
    void validateForPatchTask(Long groupId,Long taskId, Task task);
}
