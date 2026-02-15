package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.exception.duplicate.UserDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.validation.InvalidFieldValueException;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidatorImpl implements UserValidator{
    private UserRepository userRepository;

    @Override
    public void validate(User user) {
        if(userRepository.existsByAzureKey(user.getAzureKey()))
            throw new UserDuplicateException("User with azure key" +
                    user.getAzureKey() + " already exists");
    }

    @Override
    public void validateForPatch(Long id, User user) {
        if (user.getName()!=null && user.getName().isBlank()){
            throw new InvalidFieldValueException("Name cant be blank");
        }
    }

}
