package org.allsparks.mimic.observe;

import java.util.Objects;

/**
 * Immutable once-per-loop mechanism snapshot. Never used to write hardware.
 *
 * Position, velocity, and acceleration share {@link #positionUnitSymbol()}.
 * Effort values are dimensionless in {@code [-1, 1]}. Current is amperes or
 * {@link Double#NaN} when unsupported.
 */
public final class MechanismSnapshot {
    private final String mechanismId;
    private final double position;
    private final double velocity;
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

    public MechanismSnapshot(
            String mechanismId,
            double position,
            double velocity,
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
        this.mechanismId = mechanismId == null ? "" : mechanismId;
        this.position = position;
        this.velocity = velocity;
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
    }

    public String mechanismId() {
        return mechanismId;
    }

    public double position() {
        return position;
    }

    public double velocity() {
        return velocity;
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
}
