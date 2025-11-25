package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.Task;
import org.springframework.web.multipart.MultipartFile;

public interface GroupValidator extends BaseValidator<Group>{
    void validateForCreateTask(Long groupId, Long assignedId, Long reviewerId);
}
