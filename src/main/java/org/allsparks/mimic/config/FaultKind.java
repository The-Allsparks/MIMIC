package org.allsparks.mimic.config;

/**
 * Catalog fault kinds from generic-mechanism-catalog.md section 7. Each
 * value has a default {@link FaultSeverity}. This is not a detector and
 * does not write motors or servos.
 *
 * {@link #LATCH_UNKNOWN} must not auto-release: an unknown latch is not
 * disengaged. {@link #INSUFFICIENT_AMPER_GRANT} is a declared fault
 * later; it does not change output here (issue #21).
 */
public enum FaultKind {
    SENSOR_INVALID("invalid", FaultSeverity.DEGRADED, true),
    SENSOR_DISAGREEMENT("disagree", FaultSeverity.DEGRADED, true),
    UNEXPECTED_LIMIT("unexpected limit", FaultSeverity.STOP_MECHANISM, false),
    STALL("stall", FaultSeverity.STOP_MECHANISM, false),
    JAM("jam", FaultSeverity.STOP_MECHANISM, false),
    SKEW("skew", FaultSeverity.STOP_MECHANISM, false),
    FAILED_HOME("failed home", FaultSeverity.STOP_MECHANISM, false),
    TIMEOUT("timeout", FaultSeverity.DEGRADED, false),
    UNEXPECTED_MOTION("unexpected motion", FaultSeverity.STOP_MECHANISM, false),
    LOST_CALIBRATION("lost calibration", FaultSeverity.STOP_MECHANISM, false),
    INSUFFICIENT_AMPER_GRANT("insufficient AMPER grant", FaultSeverity.DEGRADED, false),
    LATCH_UNKNOWN("latch unknown", FaultSeverity.STOP_MECHANISM, false);

    private final String catalogName;
    private final FaultSeverity defaultSeverity;
    private final boolean allowsIgnoreOptional;

    FaultKind(String catalogName, FaultSeverity defaultSeverity, boolean allowsIgnoreOptional) {
        this.catalogName = catalogName;
        this.defaultSeverity = defaultSeverity;
        this.allowsIgnoreOptional = allowsIgnoreOptional;
    }

    /**
     * Phrase used in the catalog feature list. Tests lock the twelve names.
     */
    public String catalogName() {
        return catalogName;
    }

    /**
     * Severity when no {@link DegradedBehavior} is supplied. Safety-critical
     * kinds floor at {@link FaultSeverity#STOP_MECHANISM}.
     */
    public FaultSeverity defaultSeverity() {
        return defaultSeverity;
    }

    /**
     * True only for optional-sensor kinds. {@link DegradedBehavior#IGNORE_OPTIONAL}
     * may map those to {@link FaultSeverity#INFO}. Latch unknown, stall, jam,
     * and missing AMPER grant cannot be ignored this way.
     */
    public boolean allowsIgnoreOptional() {
        return allowsIgnoreOptional;
    }
}
