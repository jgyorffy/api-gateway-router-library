package com.nike.agwy.lambda;

import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.Set;

public class ValidationException extends RuntimeException {
    private final Set<ConstraintViolation> violations;

    public <T> ValidationException(String msg, Set<ConstraintViolation> violations) {
        super(msg);
        this.violations = violations;
    }

    public Set<ConstraintViolation> getViolations() {
        return Collections.unmodifiableSet(violations);
    }
}
