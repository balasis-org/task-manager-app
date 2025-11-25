package io.github.balasis.taskmanager.engine.core.validation;

public interface BaseValidator<T> {
    void validate(T type);

    void validateForPatch(Long id, T type);
}
