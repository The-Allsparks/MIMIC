package org.allsparks.mimic.observe;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import org.allsparks.mimic.clock.MimicClock;
import org.allsparks.mimic.units.MechanismUnits;

/**
 * Captures one immutable {@link MechanismSnapshot} per call. Never commands
 * hardware.
 *
 * Acceleration is a finite-difference estimate from successive usable
 * velocity samples. Missing sensors degrade to {@link MeasurementValidity}
 * rather than inventing values.
 *
 * {@link MeasurementValidity#STALE} is observer liveness, not Control Hub
 * sample age. When {@code staleAfterNanos > 0}, a capture whose start time
 * is more than that many nanoseconds after the previous capture start marks
 * numeric {@link SensorSample}s {@code STALE}. Instantaneous
 * {@link DoubleSupplier} reads cannot detect a frozen Hub cache; a timely
 * loop still reports {@code VALID}. The first capture is never {@code STALE}.
 * Limit switches and effort values are not freshness-classified in Phase 0.
 */
public final class MechanismObserver {
    private final String mechanismId;
    private final MimicClock clock;
    private final MechanismUnits units;
    private final DoubleSupplier ticks;
    private final DoubleSupplier ticksPerSecond;
    private final DoubleSupplier requestedOutput;
    private final DoubleSupplier appliedOutput;
    private final DoubleSupplier currentAmps;
    private final BooleanSupplier lowerLimitRaw;
    private final BooleanSupplier upperLimitRaw;
    private final boolean lowerLimitInverted;
    private final boolean upperLimitInverted;
    private final DoubleSupplier absoluteSensor;
    private final String absoluteUnitSymbol;
    private final DoubleSupplier redundantTicks;
    private final long staleAfterNanos;
    private final double disagreementThreshold;

    private double lastVelocity = Double.NaN;
    private long lastTimestampNanos;
    private boolean hasLastVelocity;
    private long lastCaptureNanos;
    private boolean hasLastCapture;
    private MechanismSnapshot lastSnapshot;

    public MechanismObserver(Builder builder) {
        this.mechanismId = builder.mechanismId;
        this.clock = builder.clock;
        this.units = builder.units;
        this.ticks = builder.ticks;
        this.ticksPerSecond = builder.ticksPerSecond;
        this.requestedOutput = builder.requestedOutput;
        this.appliedOutput = builder.appliedOutput;
        this.currentAmps = builder.currentAmps;
        this.lowerLimitRaw = builder.lowerLimitRaw;
        this.upperLimitRaw = builder.upperLimitRaw;
        this.lowerLimitInverted = builder.lowerLimitInverted;
        this.upperLimitInverted = builder.upperLimitInverted;
        this.absoluteSensor = builder.absoluteSensor;
        this.absoluteUnitSymbol = builder.absoluteUnitSymbol;
        this.redundantTicks = builder.redundantTicks;
        this.staleAfterNanos = builder.staleAfterNanos;
        this.disagreementThreshold = builder.disagreementThreshold;
    }

    public static Builder builder(String mechanismId, MimicClock clock, MechanismUnits units) {
        return new Builder(mechanismId, clock, units);
    }

    /** Read sensors once and return an immutable snapshot. Does not write hardware. */
    public MechanismSnapshot capture() {
        long start = clock.nanoTime();
        SensorSample position = readPosition(start);
        SensorSample velocity = readVelocity(start);
        double acceleration = estimateAcceleration(velocity, start);
        double requested = readEffort(requestedOutput, start);
        double applied = appliedOutput == null ? requested : readEffort(appliedOutput, start);
        SensorSample current = readCurrent(start);
        LimitSwitchSample lower = readLimit(lowerLimitRaw, lowerLimitInverted, start, mechanismId + ":lowerLimit");
        LimitSwitchSample upper = readLimit(upperLimitRaw, upperLimitInverted, start, mechanismId + ":upperLimit");
        SensorSample absolute = readOptional(absoluteSensor, start, mechanismId + ":absolute", absoluteUnitSymbol);
        SensorSample redundant = readRedundant(start);
        double disagreement = Double.NaN;
        boolean disagreeing = false;
        if (position.isUsable() && redundant.isUsable()) {
            disagreement = Math.abs(position.value() - redundant.value());
            disagreeing = disagreement > disagreementThreshold;
        }
        boolean sensorValid = position.isUsable() && velocity.isUsable() && !disagreeing;
        long duration = Math.max(0L, clock.nanoTime() - start);
        MechanismSnapshot snapshot = new MechanismSnapshot(
                mechanismId,
                position,
                velocity,
                acceleration,
                units.canonicalUnitSymbol(),
                requested,
                applied,
                current.value(),
                lower,
                upper,
                absolute,
                redundant,
                sensorValid,
                disagreement,
                start,
                duration);
        lastCaptureNanos = start;
        hasLastCapture = true;
        lastSnapshot = snapshot;
        return snapshot;
    }

    public MechanismSnapshot lastSnapshot() {
        return lastSnapshot;
    }

    private SensorSample readPosition(long now) {
        if (ticks == null) {
            return SensorSample.unsupported(now, mechanismId + ":position", units.canonicalUnitSymbol());
        }
        try {
            double canonical = units.ticksToCanonical(ticks.getAsDouble());
            if (Double.isNaN(canonical)) {
                return SensorSample.missing(now, mechanismId + ":position", units.canonicalUnitSymbol());
            }
            return freshness(canonical, now, mechanismId + ":position", units.canonicalUnitSymbol());
        } catch (RuntimeException ex) {
            return SensorSample.missing(now, mechanismId + ":position", units.canonicalUnitSymbol());
        }
    }

    private SensorSample readVelocity(long now) {
        if (ticksPerSecond == null) {
            return SensorSample.unsupported(now, mechanismId + ":velocity", units.canonicalUnitSymbol() + "/s");
        }
        try {
            double canonical = units.ticksPerSecondToCanonical(ticksPerSecond.getAsDouble());
            if (Double.isNaN(canonical)) {
                return SensorSample.missing(now, mechanismId + ":velocity", units.canonicalUnitSymbol() + "/s");
            }
            return freshness(canonical, now, mechanismId + ":velocity", units.canonicalUnitSymbol() + "/s");
        } catch (RuntimeException ex) {
            return SensorSample.missing(now, mechanismId + ":velocity", units.canonicalUnitSymbol() + "/s");
        }
    }

    private double estimateAcceleration(SensorSample velocity, long now) {
        if (!velocity.isUsable()) {
            hasLastVelocity = false;
            lastVelocity = Double.NaN;
            lastTimestampNanos = now;
            return Double.NaN;
        }
        double accel = Double.NaN;
        if (hasLastVelocity && now > lastTimestampNanos) {
            double dtSeconds = (now - lastTimestampNanos) / 1_000_000_000.0;
            if (dtSeconds > 0.0) {
                accel = (velocity.value() - lastVelocity) / dtSeconds;
            }
        }
        lastVelocity = velocity.value();
        lastTimestampNanos = now;
        hasLastVelocity = true;
        return accel;
    }

    private double readEffort(DoubleSupplier supplier, long now) {
        if (supplier == null) {
            return Double.NaN;
        }
        try {
            double value = supplier.getAsDouble();
            return Double.isNaN(value) ? Double.NaN : value;
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private SensorSample readCurrent(long now) {
        if (currentAmps == null) {
            return SensorSample.unsupported(now, mechanismId + ":current", "A");
        }
        try {
            double amps = currentAmps.getAsDouble();
            if (Double.isNaN(amps)) {
                return SensorSample.missing(now, mechanismId + ":current", "A");
            }
            return freshness(amps, now, mechanismId + ":current", "A");
        } catch (RuntimeException ex) {
            return SensorSample.missing(now, mechanismId + ":current", "A");
        }
    }

    private LimitSwitchSample readLimit(BooleanSupplier supplier, boolean inverted, long now, String channelId) {
        if (supplier == null) {
            return LimitSwitchSample.unsupported(now, channelId);
        }
        try {
            boolean raw = supplier.getAsBoolean();
            boolean asserted = inverted != raw;
            return new LimitSwitchSample(raw, asserted, now, MeasurementValidity.VALID, channelId);
        } catch (RuntimeException ex) {
            return LimitSwitchSample.missing(now, channelId);
        }
    }

    private SensorSample readOptional(DoubleSupplier supplier, long now, String channelId, String unitSymbol) {
        if (supplier == null) {
            return SensorSample.unsupported(now, channelId, unitSymbol);
        }
        try {
            double value = supplier.getAsDouble();
            if (Double.isNaN(value)) {
                return SensorSample.missing(now, channelId, unitSymbol);
            }
            return freshness(value, now, channelId, unitSymbol);
        } catch (RuntimeException ex) {
            return SensorSample.missing(now, channelId, unitSymbol);
        }
    }

    private SensorSample readRedundant(long now) {
        if (redundantTicks == null) {
            return SensorSample.unsupported(now, mechanismId + ":redundant", units.canonicalUnitSymbol());
        }
        try {
            double canonical = units.ticksToCanonical(redundantTicks.getAsDouble());
            if (Double.isNaN(canonical)) {
                return SensorSample.missing(now, mechanismId + ":redundant", units.canonicalUnitSymbol());
            }
            return freshness(canonical, now, mechanismId + ":redundant", units.canonicalUnitSymbol());
        } catch (RuntimeException ex) {
            return SensorSample.missing(now, mechanismId + ":redundant", units.canonicalUnitSymbol());
        }
    }

    /**
     * Classifies a numeric sample using observer liveness, not Hub sample age.
     *
     * When {@code staleAfterNanos > 0} and a previous capture exists, if
     * {@code now - lastCaptureNanos > staleAfterNanos} the sample is
     * {@link MeasurementValidity#STALE}. Equal to the threshold stays
     * {@link MeasurementValidity#VALID}. {@code staleAfterNanos <= 0} disables
     * the check. Frozen supplier values on a timely loop remain {@code VALID}.
     */
    private SensorSample freshness(double value, long now, String channelId, String unitSymbol) {
        if (staleAfterNanos > 0L && hasLastCapture) {
            long age = now - lastCaptureNanos;
            if (age > staleAfterNanos) {
                return SensorSample.stale(value, now, channelId, unitSymbol);
            }
        }
        return new SensorSample(value, now, MeasurementValidity.VALID, channelId, unitSymbol);
    }

    public static final class Builder {
        private final String mechanismId;
        private final MimicClock clock;
        private final MechanismUnits units;
        private DoubleSupplier ticks;
        private DoubleSupplier ticksPerSecond;
        private DoubleSupplier requestedOutput;
        private DoubleSupplier appliedOutput;
        private DoubleSupplier currentAmps;
        private BooleanSupplier lowerLimitRaw;
        private BooleanSupplier upperLimitRaw;
        private boolean lowerLimitInverted;
        private boolean upperLimitInverted;
        private DoubleSupplier absoluteSensor;
        private String absoluteUnitSymbol = "";
        private DoubleSupplier redundantTicks;
        private long staleAfterNanos;
        private double disagreementThreshold = Double.POSITIVE_INFINITY;

        private Builder(String mechanismId, MimicClock clock, MechanismUnits units) {
            if (mechanismId == null || mechanismId.isEmpty()) {
                throw new IllegalArgumentException("mechanismId must be non-empty");
            }
            this.mechanismId = mechanismId;
            this.clock = Objects.requireNonNull(clock, "clock");
            this.units = Objects.requireNonNull(units, "units");
        }

        public Builder ticks(DoubleSupplier ticks) {
            this.ticks = ticks;
            return this;
        }

        public Builder ticksPerSecond(DoubleSupplier ticksPerSecond) {
            this.ticksPerSecond = ticksPerSecond;
            return this;
        }

        public Builder requestedOutput(DoubleSupplier requestedOutput) {
            this.requestedOutput = requestedOutput;
            return this;
        }

        public Builder appliedOutput(DoubleSupplier appliedOutput) {
            this.appliedOutput = appliedOutput;
            return this;
        }

        public Builder currentAmps(DoubleSupplier currentAmps) {
            this.currentAmps = currentAmps;
            return this;
        }

        public Builder lowerLimit(BooleanSupplier lowerLimitRaw, boolean inverted) {
            this.lowerLimitRaw = lowerLimitRaw;
            this.lowerLimitInverted = inverted;
            return this;
        }

        public Builder upperLimit(BooleanSupplier upperLimitRaw, boolean inverted) {
            this.upperLimitRaw = upperLimitRaw;
            this.upperLimitInverted = inverted;
            return this;
        }

        public Builder absoluteSensor(DoubleSupplier absoluteSensor, String unitSymbol) {
            this.absoluteSensor = absoluteSensor;
            this.absoluteUnitSymbol = unitSymbol == null ? "" : unitSymbol;
            return this;
        }

        public Builder redundantTicks(DoubleSupplier redundantTicks) {
            this.redundantTicks = redundantTicks;
            return this;
        }

        /**
         * Max nanoseconds between consecutive {@link MechanismObserver#capture()}
         * starts before numeric samples are {@link MeasurementValidity#STALE}.
         *
         * This is observer liveness (loop-call gap), not Control Hub sample
         * age. {@code <= 0} disables the check (default {@code 0}). The first
         * capture is never stale. Instantaneous suppliers cannot detect a
         * frozen Hub cache.
         */
        public Builder staleAfterNanos(long staleAfterNanos) {
            this.staleAfterNanos = staleAfterNanos;
            return this;
        }

        public Builder disagreementThreshold(double disagreementThreshold) {
            this.disagreementThreshold = disagreementThreshold;
            return this;
        }

        public MechanismObserver build() {
            return new MechanismObserver(this);
        }
    }
}
