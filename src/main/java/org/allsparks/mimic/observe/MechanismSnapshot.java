package org.allsparks.mimic.observe;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.allsparks.mimic.config.SensorRole;

/**
 * Immutable once-per-loop mechanism snapshot. Never used to write hardware.
 *
 * Position and velocity are {@link SensorSample}s: value, unit, and
 * {@link MeasurementValidity} (including observer-liveness {@code STALE}).
 * {@link #position()} and {@link #velocity()} delegate to those samples.
 * Position, velocity, and acceleration share {@link #positionUnitSymbol()}.
 * Effort values are dimensionless in {@code [-1, 1]}. Current is amperes or
 * {@link Double#NaN} when unsupported.
 *
 * Optional named extras live beside the fixed channels. {@link #sample(String)}
 * and {@link #role(SensorRole)} look up a {@link RoleSample}. Missing names
 * and roles are {@link MeasurementValidity#UNSUPPORTED}, not a fake
 * {@code false} / not-asserted reading.
 */
public final class MechanismSnapshot {
    private final String mechanismId;
    private final SensorSample positionSample;
    private final SensorSample velocitySample;
    private final double acceleration;
    private final String positionUnitSymbol;
    private final double requestedOutput;
    private final double appliedOutput;
    private final double currentAmps;
    private final LimitSwitchSample lowerLimit;
    private final LimitSwitchSample upperLimit;
    private final SensorSample absoluteSensor;
    private final SensorSample redundantPosition;
    private final boolean sensorValid;
    private final double disagreement;
    private final long timestampNanos;
    private final long loopDurationNanos;
    private final Map<String, RoleSample> extrasByName;
    private final Map<SensorRole, RoleSample> extrasByRole;

    public MechanismSnapshot(
            String mechanismId,
            SensorSample positionSample,
            SensorSample velocitySample,
            double acceleration,
            String positionUnitSymbol,
            double requestedOutput,
            double appliedOutput,
            double currentAmps,
            LimitSwitchSample lowerLimit,
            LimitSwitchSample upperLimit,
            SensorSample absoluteSensor,
            SensorSample redundantPosition,
            boolean sensorValid,
            double disagreement,
            long timestampNanos,
            long loopDurationNanos) {
        this(
                mechanismId,
                positionSample,
                velocitySample,
                acceleration,
                positionUnitSymbol,
                requestedOutput,
                appliedOutput,
                currentAmps,
                lowerLimit,
                upperLimit,
                absoluteSensor,
                redundantPosition,
                sensorValid,
                disagreement,
                timestampNanos,
                loopDurationNanos,
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    public MechanismSnapshot(
            String mechanismId,
            SensorSample positionSample,
            SensorSample velocitySample,
            double acceleration,
            String positionUnitSymbol,
            double requestedOutput,
            double appliedOutput,
            double currentAmps,
            LimitSwitchSample lowerLimit,
            LimitSwitchSample upperLimit,
            SensorSample absoluteSensor,
            SensorSample redundantPosition,
            boolean sensorValid,
            double disagreement,
            long timestampNanos,
            long loopDurationNanos,
            Map<String, RoleSample> extrasByName,
            Map<SensorRole, RoleSample> extrasByRole) {
        this.mechanismId = mechanismId == null ? "" : mechanismId;
        this.positionSample = Objects.requireNonNull(positionSample, "positionSample");
        this.velocitySample = Objects.requireNonNull(velocitySample, "velocitySample");
        this.acceleration = acceleration;
        this.positionUnitSymbol = positionUnitSymbol == null ? "" : positionUnitSymbol;
        this.requestedOutput = requestedOutput;
        this.appliedOutput = appliedOutput;
        this.currentAmps = currentAmps;
        this.lowerLimit = Objects.requireNonNull(lowerLimit, "lowerLimit");
        this.upperLimit = Objects.requireNonNull(upperLimit, "upperLimit");
        this.absoluteSensor = Objects.requireNonNull(absoluteSensor, "absoluteSensor");
        this.redundantPosition = Objects.requireNonNull(redundantPosition, "redundantPosition");
        this.sensorValid = sensorValid;
        this.disagreement = disagreement;
        this.timestampNanos = timestampNanos;
        this.loopDurationNanos = loopDurationNanos;
        this.extrasByName = copyNamedExtras(extrasByName);
        this.extrasByRole = copyRoleExtras(extrasByRole);
    }

    public String mechanismId() {
        return mechanismId;
    }

    /** Canonical position; convenience delegate of {@link #positionSample()}. */
    public double position() {
        return positionSample.value();
    }

    /** Canonical velocity; convenience delegate of {@link #velocitySample()}. */
    public double velocity() {
        return velocitySample.value();
    }

    public SensorSample positionSample() {
        return positionSample;
    }

    public SensorSample velocitySample() {
        return velocitySample;
    }

    public double acceleration() {
        return acceleration;
    }

    public String positionUnitSymbol() {
        return positionUnitSymbol;
    }

    public double requestedOutput() {
        return requestedOutput;
    }

    public double appliedOutput() {
        return appliedOutput;
    }

    public double currentAmps() {
        return currentAmps;
    }

    public LimitSwitchSample lowerLimit() {
        return lowerLimit;
    }

    public LimitSwitchSample upperLimit() {
        return upperLimit;
    }

    public SensorSample absoluteSensor() {
        return absoluteSensor;
    }

    public SensorSample redundantPosition() {
        return redundantPosition;
    }

    /**
     * Aggregate health of required wired channels, not "every channel exists."
     *
     * True when primary position is usable, velocity is either
     * {@link MeasurementValidity#UNSUPPORTED} or usable, and redundant
     * encoders are not disagreeing. Omitted {@code ticks} keeps this false.
     * {@link #absoluteSensor()} is optional and does not substitute for
     * primary pose.
     */
    public boolean sensorValid() {
        return sensorValid;
    }

    public double disagreement() {
        return disagreement;
    }

    public long timestampNanos() {
        return timestampNanos;
    }

    public long loopDurationNanos() {
        return loopDurationNanos;
    }

    /**
     * Optional extras keyed by team-owned channel name. Empty when unused.
     * Does not replace {@link #positionSample()}, {@link #velocitySample()},
     * or the fixed limit fields.
     */
    public Map<String, RoleSample> extraSamples() {
        return extrasByName;
    }

    /**
     * Named extra by team-owned channel name. Missing names are
     * {@link MeasurementValidity#UNSUPPORTED} on both sides of the
     * {@link RoleSample}, not a fake {@code false}.
     */
    public RoleSample sample(String name) {
        if (name == null || name.isEmpty()) {
            return RoleSample.unsupported(timestampNanos, mechanismId + ":");
        }
        RoleSample extra = extrasByName.get(name);
        if (extra != null) {
            return extra;
        }
        return RoleSample.unsupported(timestampNanos, mechanismId + ":" + name);
    }

    /**
     * Observation for a library sensor role. Fixed pose/velocity/limit
     * channels stay available here for compatibility. Extra optional
     * suppliers fill roles that do not already have a first-class field.
     * Missing roles are {@link MeasurementValidity#UNSUPPORTED}, not a
     * fake {@code false} / not-asserted reading.
     */
    public RoleSample role(SensorRole role) {
        if (role == null) {
            return RoleSample.unsupported(timestampNanos, mechanismId + ":role");
        }
        RoleSample firstClass = firstClassRole(role);
        if (firstClass != null) {
            return firstClass;
        }
        RoleSample extra = extrasByRole.get(role);
        if (extra != null) {
            return extra;
        }
        return RoleSample.unsupported(timestampNanos, mechanismId + ":" + role.name());
    }

    private RoleSample firstClassRole(SensorRole role) {
        if (role == SensorRole.RELATIVE_POSITION) {
            return RoleSample.of(positionSample);
        }
        if (role == SensorRole.VELOCITY) {
            return RoleSample.of(velocitySample);
        }
        if (role == SensorRole.ABSOLUTE_POSITION) {
            return RoleSample.of(absoluteSensor);
        }
        if (role == SensorRole.REDUNDANT_POSITION) {
            return RoleSample.of(redundantPosition);
        }
        if (role == SensorRole.RETRACT_LIMIT) {
            return RoleSample.of(lowerLimit);
        }
        if (role == SensorRole.EXTEND_LIMIT) {
            return RoleSample.of(upperLimit);
        }
        return null;
    }

    private static Map<String, RoleSample> copyNamedExtras(Map<String, RoleSample> extrasByName) {
        if (extrasByName == null || extrasByName.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(extrasByName));
    }

    private static Map<SensorRole, RoleSample> copyRoleExtras(Map<SensorRole, RoleSample> extrasByRole) {
        if (extrasByRole == null || extrasByRole.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new EnumMap<>(extrasByRole));
    }
}
