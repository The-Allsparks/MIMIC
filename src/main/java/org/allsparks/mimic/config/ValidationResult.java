package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result of configuration validation. */
public final class ValidationResult {
    private final List<ValidationIssue> issues;

    private ValidationResult(List<ValidationIssue> issues) {
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
    }

    public static ValidationResult ok() {
        return new ValidationResult(Collections.emptyList());
    }

    public static ValidationResult of(List<ValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return ok();
        }
        return new ValidationResult(issues);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public List<ValidationIssue> issues() {
        return issues;
    }

    public boolean hasCode(String code) {
        for (ValidationIssue issue : issues) {
            if (issue.code().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String summary() {
        if (valid()) {
            return "valid";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < issues.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(issues.get(i).toString());
        }
        return builder.toString();
    }
}
