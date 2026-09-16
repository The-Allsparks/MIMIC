package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.allsparks.mimic.units.DirectionSign;

/**
 * Immutable declaration of how a later phase may home. Phase 0 does not run
 * homing: a contract is not permission to move, does not change
 * {@code MimicSession.calibrationState()}, and does not write motors or
 * servos.
 *
 * {@link CalibrationStrategy#HARD_STOP_CURRENT} stays an explicit blocked
 * strategy. Declaring this object must not enable current-home motion.
 */
public final class CalibrationContract {
    private final CalibrationStrategy strategy;
    private final DirectionSign direction;
    private final Double maxTravel;
    private final Long timeoutNanos;
    private final long debounceNanos;
    private final EncoderResetPolicy encoderResetPolicy;

    private CalibrationContract(Builder builder) {
        this.strategy = builder.strategy;
        this.direction = builder.direction == null ? DirectionSign.POSITIVE : builder.direction;
        this.maxTravel = builder.maxTravel;
        this.timeoutNanos = builder.timeoutNanos;
        this.debounceNanos = builder.debounceNanos;
        this.encoderResetPolicy =
                builder.encoderResetPolicy == null
                        ? EncoderResetPolicy.UNSPECIFIED
                        : builder.encoderResetPolicy;
    }

    public static Builder homeSwitch() {
        return new Builder(CalibrationStrategy.HOME_SWITCH);
    }

    public static Builder indexPulse() {
        return new Builder(CalibrationStrategy.INDEX_PULSE);
    }

    public static Builder hardStopCurrent() {
        return new Builder(CalibrationStrategy.HARD_STOP_CURRENT);
    }

    public static Builder of(CalibrationStrategy strategy) {
        return new Builder(strategy);
    }

    public CalibrationStrategy strategy() {
        return strategy;
    }

    public DirectionSign direction() {
        return direction;
    }

    public Double maxTravel() {
        return maxTravel;
    }

    public Long timeoutNanos() {
        return timeoutNanos;
    }

    public long debounceNanos() {
        return debounceNanos;
    }

    public EncoderResetPolicy encoderResetPolicy() {
        return encoderResetPolicy;
    }

    /**
     * Always false. Declaring bounds does not authorize homing or any
     * actuator write. {@link CalibrationStrategy#HARD_STOP_CURRENT} remains
     * blocked even when timeout and travel are present.
     */
    public boolean permitsMotion() {
        return false;
    }

    public boolean isHardStopCurrentBlocked() {
        return strategy == CalibrationStrategy.HARD_STOP_CURRENT;
    }

    public boolean hasTimeout() {
        return timeoutNanos != null && timeoutNanos > 0L;
    }

    public boolean hasMaxTravel() {
        return maxTravel != null && Double.isFinite(maxTravel) && maxTravel > 0.0;
    }

    public boolean requiresTravelAndTimeout() {
        return requiresTravelAndTimeout(strategy);
    }

    public static boolean requiresTravelAndTimeout(CalibrationStrategy strategy) {
        return strategy == CalibrationStrategy.HOME_SWITCH
                || strategy == CalibrationStrategy.INDEX_PULSE
                || strategy == CalibrationStrategy.HARD_STOP_CURRENT;
    }

    public ValidationResult validate() {
        List<ValidationIssue> issues = new ArrayList<>();
        collectBoundIssues(issues);
        return ValidationResult.of(issues);
    }

    void collectBoundIssues(List<ValidationIssue> issues) {
        if (!requiresTravelAndTimeout()) {
            return;
        }
        if (!hasTimeout()) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED,
                            "timeoutNanos",
                            "homing-capable calibration contract requires a timeout"));
        }
        if (!hasMaxTravel()) {
            issues.add(
                    new ValidationIssue(
                            ConfigurationValidator.CALIBRATION_MAX_TRAVEL_REQUIRED,
                            "maxTravel",
                            "homing-capable calibration contract requires max travel"));
        }
    }

    public static final class Builder {
        private final CalibrationStrategy strategy;
        private DirectionSign direction = DirectionSign.POSITIVE;
        private Double maxTravel;
        private Long timeoutNanos;
        private long debounceNanos;
        private EncoderResetPolicy encoderResetPolicy = EncoderResetPolicy.UNSPECIFIED;

        private Builder(CalibrationStrategy strategy) {
            this.strategy = strategy == null ? CalibrationStrategy.NONE : strategy;
        }

        public Builder direction(DirectionSign direction) {
            this.direction = direction == null ? DirectionSign.POSITIVE : direction;
            return this;
        }

        public Builder maxTravel(double maxTravel) {
            this.maxTravel = maxTravel;
            return this;
        }

        public Builder timeout(long timeoutNanos) {
            this.timeoutNanos = timeoutNanos;
            return this;
        }

        public Builder debounce(long debounceNanos) {
            this.debounceNanos = debounceNanos;
            return this;
        }

        public Builder encoderResetPolicy(EncoderResetPolicy encoderResetPolicy) {
            this.encoderResetPolicy =
                    encoderResetPolicy == null ? EncoderResetPolicy.UNSPECIFIED : encoderResetPolicy;
            return this;
        }

        public ValidationResult validate() {
            return new CalibrationContract(this).validate();
        }

        public CalibrationContract build() {
            return new CalibrationContract(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CalibrationContract)) {
            return false;
        }
        CalibrationContract that = (CalibrationContract) other;
        return debounceNanos == that.debounceNanos
                && strategy == that.strategy
                && direction == that.direction
                && encoderResetPolicy == that.encoderResetPolicy
                && Objects.equals(maxTravel, that.maxTravel)
                && Objects.equals(timeoutNanos, that.timeoutNanos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                strategy, direction, maxTravel, timeoutNanos, debounceNanos, encoderResetPolicy);
    }
}
