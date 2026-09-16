package org.allsparks.mimic.templates;

import java.util.Objects;

/**
 * Catalog entry for one named axis. Pair it with a {@code MechanismObserver}
 * that uses the same {@code mechanismId}. This object never writes hardware.
 *
 * Compose a scoring assembly as several blueprints (for example turret + hood
 * + flywheel), each with its own observer.
 */
public final class MechanismBlueprint {
    private final String mechanismId;
    private final MechanismConstruct construct;

    public MechanismBlueprint(String mechanismId, MechanismConstruct construct) {
        if (mechanismId == null || mechanismId.isEmpty()) {
            throw new IllegalArgumentException("mechanismId must be non-empty");
        }
        this.mechanismId = mechanismId;
        this.construct = Objects.requireNonNull(construct, "construct");
    }

    public static MechanismBlueprint of(String mechanismId, MechanismConstruct construct) {
        return new MechanismBlueprint(mechanismId, construct);
    }

    public String mechanismId() {
        return mechanismId;
    }

    public MechanismConstruct construct() {
        return construct;
    }

    public MechanismFamily family() {
        return construct.family();
    }

    public MechanismMotionKind motionKind() {
        return construct.motionKind();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MechanismBlueprint)) {
            return false;
        }
        MechanismBlueprint that = (MechanismBlueprint) other;
        return mechanismId.equals(that.mechanismId) && construct == that.construct;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mechanismId, construct);
    }

    @Override
    public String toString() {
        return mechanismId + "[" + construct.name() + "]";
    }
}
