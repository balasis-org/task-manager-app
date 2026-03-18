package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.model.User;

// marker interface — exists so the service can inject UserValidator
// without depending on the concrete impl directly
public interface UserValidator extends BaseValidator<User> {
}
