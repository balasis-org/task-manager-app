package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.Group;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GroupValidatorImpl implements GroupValidator{
    @Override
    public Group validate(Group group) {
        return group;
    }

    @Override
    public Group validateForUpdate(Long id, Group group) {
        return group;
    }
}
