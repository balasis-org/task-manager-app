package io.github.balasis.taskmanager.context.web.validation;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.stereotype.Component;

@Component
public class ResourceDataValidator extends BaseComponent {
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();

    public <T extends BaseInboundResource> void validateResourceData(T resourceToBeValidated) {
        var constraintViolations = validator.validate(resourceToBeValidated);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

}
