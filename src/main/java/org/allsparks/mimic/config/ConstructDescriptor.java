package org.allsparks.mimic.config;

import java.util.Locale;
import java.util.Objects;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.templates.MechanismFamily;
import org.allsparks.mimic.templates.MechanismMotionKind;

/**
 * Immutable construct identity. Standard presets wrap {@link MechanismConstruct}.
 * Custom layouts do not require editing that enum.
 *
 * This is metadata. It does not command hardware.
 */
public final class ConstructDescriptor {
    private final String id;
    private final MechanismFamily family;
    private final MechanismMotionKind motionKind;
    private final String summary;
    private final MechanismConstruct standardConstruct;

    private ConstructDescriptor(
            String id,
            MechanismFamily family,
            MechanismMotionKind motionKind,
            String summary,
            MechanismConstruct standardConstruct) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("construct id must be non-empty");
        }
        this.id = id;
        this.family = Objects.requireNonNull(family, "family");
        this.motionKind = Objects.requireNonNull(motionKind, "motionKind");
        this.summary = summary == null ? "" : summary;
        this.standardConstruct = standardConstruct;
    }

    public static ConstructDescriptor standard(MechanismConstruct construct) {
        Objects.requireNonNull(construct, "construct");
        return new ConstructDescriptor(
                construct.name(),
                construct.family(),
                construct.motionKind(),
                construct.summary(),
                construct);
    }

    public static ConstructDescriptor custom(
            String id, MechanismFamily family, MechanismMotionKind motionKind, String summary) {
        return new ConstructDescriptor(id, family, motionKind, summary, null);
    }

    public String id() {
        return id;
    }

    public MechanismFamily family() {
        return family;
    }

    public MechanismMotionKind motionKind() {
        return motionKind;
    }

    public String summary() {
        return summary;
    }

    public boolean isStandard() {
        return standardConstruct != null;
    }

    /** Null when this descriptor is custom. */
    public MechanismConstruct standardConstruct() {
        return standardConstruct;
    }

    public String idLowerCase() {
        return id.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConstructDescriptor)) {
            return false;
        }
        ConstructDescriptor that = (ConstructDescriptor) other;
        return id.equals(that.id)
                && family == that.family
                && motionKind == that.motionKind
                && summary.equals(that.summary)
                && standardConstruct == that.standardConstruct;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, family, motionKind, summary, standardConstruct);
    }

    @Override
    public String toString() {
        return isStandard() ? "standard:" + id : "custom:" + id;
    }
}
