package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable declaration of how far independently sensed actuators may
 * disagree before a later phase stops the mechanism. Phase 0 does not
 * synchronize motors: a contract is not anti-racking correction, does
 * not command followers, and does not write motors or servos.
 *
 * Mechanically linked motors share one command. Do not attach this
 * object to a common shaft. Two independent towers need two sensors.
 * {@code actuatorCount > 1} is not this contract.
 *
 * <p>Sketch:
 * {@code SyncContract.maxDisagreement(canonicalUnits).action(DegradedBehavior.STOP_MECHANISM)}.
 */
public final class SyncContract {
    private final double maxDisagreement;
    private final DegradedBehavior action;

    private SyncContract(Builder builder) {
        this.maxDisagreement = builder.maxDisagreement;
        this.action =
                builder.action == null ? DegradedBehavior.STOP_MECHANISM : builder.action;
    }

    /**
     * Disagreement limit in the mechanism's canonical units (for example
     * millimeters). Presence does not enable Phase 5 synchronization.
     */
    public static Builder maxDisagreement(double canonicalUnits) {
        return new Builder(canonicalUnits);
    }

    public double maxDisagreement() {
        return maxDisagreement;
    }

    public DegradedBehavior action() {
        return action;
    }

    /**
     * Always false. Declaring a disagreement limit does not authorize
     * actuation, follower commands, or Phase 5 correction.
     */
    public boolean permitsMotion() {
        return false;
    }

    /**
     * Always false. A disagreement limit is not side-to-side correction.
     */
    public boolean appliesSideCorrection() {
        return false;
    }

    public boolean hasUsableMaxDisagreement() {
        return Double.isFinite(maxDisagreement) && maxDisagreement > 0.0;
    }

    public ValidationResult validate() {
        List<ValidationIssue> issues = new ArrayList<>();
        collectBoundIssues(issues);
        return ValidationResult.of(issues);
    }

    void collectBoundIssues(List<ValidationIssue> issues) {
        if (!hasUsableMaxDisagreement()) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.SYNC_MAX_DISAGREEMENT_REQUIRED,
                            "maxDisagreement",
                            "sync contract requires a finite max disagreement greater than 0"));
        }
    }

    public static final class Builder {
        private final double maxDisagreement;
        private DegradedBehavior action = DegradedBehavior.STOP_MECHANISM;

        private Builder(double maxDisagreement) {
            this.maxDisagreement = maxDisagreement;
        }

        /**
         * Declared response when disagreement exceeds the limit. Reuses
         * {@link DegradedBehavior}; typical value is
         * {@link DegradedBehavior#STOP_MECHANISM}. Does not stop motors now.
         */
        public Builder action(DegradedBehavior action) {
            this.action = action == null ? DegradedBehavior.STOP_MECHANISM : action;
            return this;
        }

        public ValidationResult validate() {
            return new SyncContract(this).validate();
        }

        public SyncContract build() {
            return new SyncContract(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SyncContract)) {
            return false;
        }
        SyncContract that = (SyncContract) other;
        return Double.compare(that.maxDisagreement, maxDisagreement) == 0 && action == that.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxDisagreement, action);
    }
}
