package org.allsparks.mimic.config;

import java.util.Objects;

/**
 * Policy table from {@link FaultKind} plus {@link DegradedBehavior} onto
 * {@link FaultSeverity}. Sketch:
 * {@code FaultPolicy.severity(FaultKind.SENSOR_DISAGREEMENT, DegradedBehavior.STOP_MECHANISM)}.
 *
 * Phase 0 looks up severities only. This is not Phase 8 recovery motion
 * (issue #20), not an AMPER grant that changes output (issue #21), and
 * not {@code MimicFeatureFlags.phase8Faults}. {@link org.allsparks.mimic.MimicSession}
 * must not consult this table. Latch unknown never auto-releases.
 */
public final class FaultPolicy {
    private static final FaultPolicy DEFAULTS = new FaultPolicy();

    private FaultPolicy() {}

    public static FaultPolicy defaults() {
        return DEFAULTS;
    }

    /**
     * Default severity for {@code kind} with no degraded declaration.
     */
    public static FaultSeverity severity(FaultKind kind) {
        Objects.requireNonNull(kind, "kind");
        return kind.defaultSeverity();
    }

    /**
     * Combined lookup. Optional-sensor kinds may log-only on
     * {@link DegradedBehavior#IGNORE_OPTIONAL}. Other kinds floor at their
     * default, so ignore cannot hide stall, jam, or latch unknown.
     */
    public static FaultSeverity severity(FaultKind kind, DegradedBehavior behavior) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(behavior, "behavior");
        if (behavior == DegradedBehavior.IGNORE_OPTIONAL && kind.allowsIgnoreOptional()) {
            return FaultSeverity.INFO;
        }
        return FaultSeverity.stricter(
                kind.defaultSeverity(), FaultSeverity.fromDegradedBehavior(behavior));
    }

    /**
     * Always false. An unknown latch is not a command to disengage, even
     * when the sensor role is optional or {@link DegradedBehavior#IGNORE_OPTIONAL}.
     */
    public static boolean autoReleases(FaultKind kind) {
        Objects.requireNonNull(kind, "kind");
        return false;
    }

    /**
     * Always false. Ignore-optional still does not release a latch.
     */
    public static boolean autoReleases(FaultKind kind, DegradedBehavior behavior) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(behavior, "behavior");
        return false;
    }

    /**
     * Always false. A policy row is not permission to move.
     */
    public boolean permitsMotion() {
        return false;
    }

    /**
     * Always false. Reverse-clear and other recovery motion stay issue #20.
     */
    public boolean permitsRecoveryMotion() {
        return false;
    }

    /**
     * Always false. Missing AMPER grant is named here and does not clip
     * or zero output (issue #21).
     */
    public boolean changesOutput() {
        return false;
    }
}
