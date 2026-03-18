package io.github.balasis.taskmanager.engine.core.validation;

// common interface for all entity validators. every domain type that needs
// pre-save validation gets a Validator<T> implementation. validate() is for
// creates, validateForPatch() is for partial updates where only changed
// fields need checking (null = "not being changed").
public interface BaseValidator<T> {
    void validate(T type);

    void validateForPatch(Long id, T type);
}
