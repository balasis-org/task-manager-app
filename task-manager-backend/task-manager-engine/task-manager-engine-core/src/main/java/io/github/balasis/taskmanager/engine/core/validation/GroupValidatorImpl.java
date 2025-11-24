package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.engine.core.repository.GroupMembershipRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.EffectiveCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GroupValidatorImpl implements GroupValidator{
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;

    @Override
    public void validate(Group group) {

        boolean doesGroupExistWithSameName=groupRepository.existsByNameAndOwner_Id(
                group.getName(),effectiveCurrentUser.getUserId());
        if(doesGroupExistWithSameName){
            throw new GroupDuplicateException("You already have a group with that name");
        };
    }

    @Override
    public void validateForUpdate(Long id, Group group) {


    }
}
