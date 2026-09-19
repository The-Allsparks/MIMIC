package org.allsparks.mimic.config;

import java.util.Collections;
import java.util.List;

/** Thrown when {@link MechanismConfiguration} validation fails. */
public final class InvalidMechanismConfigurationException extends IllegalArgumentException {
    private final ValidationResult result;

    public InvalidMechanismConfigurationException(ValidationResult result) {
        super(result == null ? "invalid configuration" : result.summary());
        this.result = result == null ? ValidationResult.of(Collections.emptyList()) : result;
    }

    public ValidationResult result() {
        return result;
    }

    public List<ValidationIssue> issues() {
        return result.issues();
    }
}
