package org.allsparks.mimic.config;

import java.util.Objects;

/**
 * Named sensor declaration. The name is team-owned; the role is library-owned.
 * No FTC device type is stored here.
 */
public final class SensorDeclaration {
    private final String name;
    private final SensorRole role;
    private final SensorRequiredness requiredness;

    public SensorDeclaration(String name, SensorRole role, SensorRequiredness requiredness) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("sensor name must be non-empty");
        }
        this.name = name;
        this.role = Objects.requireNonNull(role, "role");
        this.requiredness = requiredness == null ? SensorRequiredness.OPTIONAL : requiredness;
    }

    public static SensorDeclaration optional(String name, SensorRole role) {
        return new SensorDeclaration(name, role, SensorRequiredness.OPTIONAL);
    }

    public static SensorDeclaration required(String name, SensorRole role) {
        return new SensorDeclaration(name, role, SensorRequiredness.REQUIRED);
    }

    public String name() {
        return name;
    }

    public SensorRole role() {
        return role;
    }

    public SensorRequiredness requiredness() {
        return requiredness;
    }

    public boolean required() {
        return requiredness == SensorRequiredness.REQUIRED;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SensorDeclaration)) {
            return false;
        }
        SensorDeclaration that = (SensorDeclaration) other;
        return name.equals(that.name) && role == that.role && requiredness == that.requiredness;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, role, requiredness);
    }
}
