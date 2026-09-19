package org.allsparks.mimic.config;

import java.util.Objects;

/** One configuration problem with a stable code for tests. */
public final class ValidationIssue {
    private final String code;
    private final String field;
    private final String message;

    public ValidationIssue(String code, String field, String message) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code must be non-empty");
        }
        this.code = code;
        this.field = field == null ? "" : field;
        this.message = message == null ? "" : message;
    }

    public String code() {
        return code;
    }

    public String field() {
        return field;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidationIssue)) {
            return false;
        }
        ValidationIssue that = (ValidationIssue) other;
        return code.equals(that.code) && field.equals(that.field) && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, field, message);
    }

    @Override
    public String toString() {
        return code + " [" + field + "]: " + message;
    }
}
