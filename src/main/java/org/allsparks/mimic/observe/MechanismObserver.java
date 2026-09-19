package org.allsparks.mimic.observe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import org.allsparks.contracts.input.InputPriority;
import org.allsparks.contracts.input.InputRegistrar;
import org.allsparks.contracts.input.InputRequirements;
import org.allsparks.contracts.input.InputValues;
import org.allsparks.contracts.input.SamplingPolicy;
import org.allsparks.contracts.input.SignalKey;
import org.allsparks.mimic.clock.MimicClock;
import org.allsparks.mimic.config.SensorRole;
import org.allsparks.mimic.input.InputValuesReads;
import org.allsparks.mimic.input.MimicSignals;
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
 *
 * When a sampler owns Hub I/O, call {@link Builder#declareInputs} then bind
 * {@link Builder#physicalDoubles}/{@link Builder#physicalBooleans} then
 * {@link Builder#readFrom}. {@code capture()} then reads
 * {@link org.allsparks.contracts.input.InputValues} and does not invoke the
 * physical getters. MIMIC does not import PULSE.
 *
 * Optional named extras ({@code namedSample} / {@code namedLimit}) are copied
 * onto the snapshot by name and {@link SensorRole}. They do not replace
 * position, velocity, or the fixed limit fields and do not write hardware.
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
    private final List<NamedNumericBinding> namedNumerics;
    private final List<NamedDigitalBinding> namedDigitals;
    private final InputValues inputValues;
    private final SignalKey<Double> ticksKey;
    private final SignalKey<Double> ticksPerSecondKey;
    private final SignalKey<Double> currentKey;
    private final SignalKey<Boolean> lowerLimitKey;
    private final SignalKey<Boolean> upperLimitKey;
    private final SignalKey<Double> absoluteKey;
    private final SignalKey<Double> redundantKey;

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
        this.namedNumerics = Collections.unmodifiableList(new ArrayList<>(builder.namedNumerics));
        this.namedDigitals = Collections.unmodifiableList(new ArrayList<>(builder.namedDigitals));
        this.inputValues = builder.inputValues;
        this.ticksKey = builder.ticksKey;
        this.ticksPerSecondKey = builder.ticksPerSecondKey;
        this.currentKey = builder.currentKey;
        this.lowerLimitKey = builder.lowerLimitKey;
        this.upperLimitKey = builder.upperLimitKey;
        this.absoluteKey = builder.absoluteKey;
        this.redundantKey = builder.redundantKey;
    }

    public static Builder builder(String mechanismId, MimicClock clock, MechanismUnits units) {
        return new Builder(mechanismId, clock, units);
    }

    /**
     * Read sensors once and return an immutable snapshot. Does not write hardware.
     *
     * {@link MechanismSnapshot#sensorValid()} is true when required wired
     * channels are usable and redundant encoders are not disagreeing. Primary
     * position is required: omitted {@code ticks} is
     * {@link MeasurementValidity#UNSUPPORTED} and keeps the flag false.
     * Velocity is required only when {@code ticksPerSecond} is wired;
     * {@code UNSUPPORTED} velocity does not clear the flag. Wired but
     * {@code MISSING}, {@code STALE}, NaN, or throwing velocity still does.
     * Analog-only mechanisms wire the mapped analog value as {@code ticks}
     * and omit {@code ticksPerSecond}; {@code absoluteSensor} is an optional
     * extra channel, not a substitute primary pose. Velocity is never
     * inferred from position.
     */
    public MechanismSnapshot capture() {
        long start = clock.nanoTime();
        SensorSample position = readPosition(start);
        SensorSample velocity = readVelocity(start);
        double acceleration = estimateAcceleration(velocity, start);
        double requested = readEffort(requestedOutput, start);
        double applied = appliedOutput == null ? requested : readEffort(appliedOutput, start);
        SensorSample current = readCurrent(start);
        LimitSwitchSample lower =
                readLimit(lowerLimitRaw, lowerLimitKey, lowerLimitInverted, start, mechanismId + ":lowerLimit");
        LimitSwitchSample upper =
                readLimit(upperLimitRaw, upperLimitKey, upperLimitInverted, start, mechanismId + ":upperLimit");
        SensorSample absolute =
                readOptional(absoluteSensor, absoluteKey, start, mechanismId + ":absolute", absoluteUnitSymbol);
        SensorSample redundant = readRedundant(start);
        double disagreement = Double.NaN;
        boolean disagreeing = false;
        if (position.isUsable() && redundant.isUsable()) {
            disagreement = Math.abs(position.value() - redundant.value());
            disagreeing = disagreement > disagreementThreshold;
        }
        boolean sensorValid =
                position.isUsable()
                        && (velocity.validity() == MeasurementValidity.UNSUPPORTED
                                || velocity.isUsable())
                        && !disagreeing;
        Map<String, RoleSample> extrasByName = readNamedExtras(start);
        Map<SensorRole, RoleSample> extrasByRole = indexExtrasByRole(extrasByName);
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
                duration,
                extrasByName,
                extrasByRole);
        lastCaptureNanos = start;
        hasLastCapture = true;
        lastSnapshot = snapshot;
        return snapshot;
    }

    public MechanismSnapshot lastSnapshot() {
        return lastSnapshot;
    }

    private SensorSample readPosition(long now) {
        if (ticksKey != null && inputValues != null) {
            return convertTicks(
                    InputValuesReads.numeric(
                            inputValues, ticksKey, now, mechanismId + ":position", units.canonicalUnitSymbol()),
                    now,
                    mechanismId + ":position",
                    units.canonicalUnitSymbol(),
                    true);
        }
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
        String unit = units.canonicalUnitSymbol() + "/s";
        if (ticksPerSecondKey != null && inputValues != null) {
            return convertTicks(
                    InputValuesReads.numeric(
                            inputValues, ticksPerSecondKey, now, mechanismId + ":velocity", unit),
                    now,
                    mechanismId + ":velocity",
                    unit,
                    false);
        }
        if (ticksPerSecond == null) {
            return SensorSample.unsupported(now, mechanismId + ":velocity", unit);
        }
        try {
            double canonical = units.ticksPerSecondToCanonical(ticksPerSecond.getAsDouble());
            if (Double.isNaN(canonical)) {
                return SensorSample.missing(now, mechanismId + ":velocity", unit);
            }
            return freshness(canonical, now, mechanismId + ":velocity", unit);
        } catch (RuntimeException ex) {
            return SensorSample.missing(now, mechanismId + ":velocity", unit);
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
        if (currentKey != null && inputValues != null) {
            return publishedNumeric(
                    InputValuesReads.numeric(inputValues, currentKey, now, mechanismId + ":current", "A"),
                    now,
                    mechanismId + ":current",
                    "A");
        }
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
        return readLimit(supplier, null, inverted, now, channelId);
    }

    private LimitSwitchSample readLimit(
            BooleanSupplier supplier, SignalKey<Boolean> key, boolean inverted, long now, String channelId) {
        if (key != null && inputValues != null) {
            return InputValuesReads.digital(inputValues, key, inverted, now, channelId);
        }
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

    private SensorSample readOptional(
            DoubleSupplier supplier, long now, String channelId, String unitSymbol) {
        return readOptional(supplier, null, now, channelId, unitSymbol);
    }

    private SensorSample readOptional(
            DoubleSupplier supplier, SignalKey<Double> key, long now, String channelId, String unitSymbol) {
        if (key != null && inputValues != null) {
            return publishedNumeric(InputValuesReads.numeric(inputValues, key, now, channelId, unitSymbol), now, channelId, unitSymbol);
        }
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
        if (redundantKey != null && inputValues != null) {
            return convertTicks(
                    InputValuesReads.numeric(
                            inputValues,
                            redundantKey,
                            now,
                            mechanismId + ":redundant",
                            units.canonicalUnitSymbol()),
                    now,
                    mechanismId + ":redundant",
                    units.canonicalUnitSymbol(),
                    true);
        }
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
     * Convert published encoder ticks (or ticks/s) into canonical units. PULSE
     * {@code STALE} stays stale. {@code VALID} still applies observer-liveness.
     */
    private SensorSample convertTicks(
            SensorSample raw, long now, String channelId, String unitSymbol, boolean positionNotVelocity) {
        if (raw.validity() != MeasurementValidity.VALID && raw.validity() != MeasurementValidity.STALE) {
            return new SensorSample(Double.NaN, raw.capturedAtNanos(), raw.validity(), channelId, unitSymbol);
        }
        double canonical = positionNotVelocity
                ? units.ticksToCanonical(raw.value())
                : units.ticksPerSecondToCanonical(raw.value());
        if (Double.isNaN(canonical)) {
            return SensorSample.missing(now, channelId, unitSymbol);
        }
        if (raw.validity() == MeasurementValidity.STALE) {
            return SensorSample.stale(canonical, raw.capturedAtNanos(), channelId, unitSymbol);
        }
        return freshness(canonical, now, channelId, unitSymbol);
    }

    private SensorSample publishedNumeric(SensorSample raw, long now, String channelId, String unitSymbol) {
        if (raw.validity() != MeasurementValidity.VALID && raw.validity() != MeasurementValidity.STALE) {
            return new SensorSample(Double.NaN, raw.capturedAtNanos(), raw.validity(), channelId, unitSymbol);
        }
        if (raw.validity() == MeasurementValidity.STALE) {
            return SensorSample.stale(raw.value(), raw.capturedAtNanos(), channelId, unitSymbol);
        }
        return freshness(raw.value(), now, channelId, unitSymbol);
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

    private Map<String, RoleSample> readNamedExtras(long now) {
        if (namedNumerics.isEmpty() && namedDigitals.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, RoleSample> extras = new LinkedHashMap<>();
        for (NamedNumericBinding binding : namedNumerics) {
            extras.put(
                    binding.name,
                    RoleSample.of(
                            readOptional(
                                    binding.supplier,
                                    binding.key,
                                    now,
                                    mechanismId + ":" + binding.name,
                                    binding.unitSymbol)));
        }
        for (NamedDigitalBinding binding : namedDigitals) {
            extras.put(
                    binding.name,
                    RoleSample.of(
                            readLimit(
                                    binding.supplier,
                                    binding.key,
                                    binding.inverted,
                                    now,
                                    mechanismId + ":" + binding.name)));
        }
        return extras;
    }

    private Map<SensorRole, RoleSample> indexExtrasByRole(Map<String, RoleSample> extrasByName) {
        if (extrasByName.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<SensorRole, RoleSample> extrasByRole = new EnumMap<>(SensorRole.class);
        indexBindings(extrasByName, extrasByRole, namedNumerics);
        indexBindings(extrasByName, extrasByRole, namedDigitals);
        return extrasByRole;
    }

    private static void indexBindings(
            Map<String, RoleSample> extrasByName,
            Map<SensorRole, RoleSample> extrasByRole,
            List<? extends NamedBinding> bindings) {
        for (NamedBinding binding : bindings) {
            if (extrasByRole.containsKey(binding.role())) {
                continue;
            }
            RoleSample sample = extrasByName.get(binding.name());
            if (sample != null) {
                extrasByRole.put(binding.role(), sample);
            }
        }
    }

    private interface NamedBinding {
        String name();

        SensorRole role();
    }

    private static final class NamedNumericBinding implements NamedBinding {
        private final String name;
        private final SensorRole role;
        private final DoubleSupplier supplier;
        private final SignalKey<Double> key;
        private final String unitSymbol;

        private NamedNumericBinding(
                String name,
                SensorRole role,
                DoubleSupplier supplier,
                SignalKey<Double> key,
                String unitSymbol) {
            this.name = name;
            this.role = role;
            this.supplier = supplier;
            this.key = key;
            this.unitSymbol = unitSymbol;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public SensorRole role() {
            return role;
        }
    }

    private static final class NamedDigitalBinding implements NamedBinding {
        private final String name;
        private final SensorRole role;
        private final BooleanSupplier supplier;
        private final SignalKey<Boolean> key;
        private final boolean inverted;

        private NamedDigitalBinding(
                String name,
                SensorRole role,
                BooleanSupplier supplier,
                SignalKey<Boolean> key,
                boolean inverted) {
            this.name = name;
            this.role = role;
            this.supplier = supplier;
            this.key = key;
            this.inverted = inverted;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public SensorRole role() {
            return role;
        }
    }

    /**
     * Physical double getter TeamCode must bind on a sampler (for example
     * {@code Pulse.bindDouble}). MIMIC does not bind it — that would pull PULSE
     * into this library.
     */
    public static final class PhysicalDouble {
        private final SignalKey<Double> key;
        private final DoubleSupplier getter;

        PhysicalDouble(SignalKey<Double> key, DoubleSupplier getter) {
            this.key = Objects.requireNonNull(key, "key");
            this.getter = Objects.requireNonNull(getter, "getter");
        }

        public SignalKey<Double> key() {
            return key;
        }

        public DoubleSupplier getter() {
            return getter;
        }
    }

    /**
     * Physical boolean getter TeamCode must bind on a sampler (for example
     * {@code Pulse.bindBoolean}).
     */
    public static final class PhysicalBoolean {
        private final SignalKey<Boolean> key;
        private final BooleanSupplier getter;

        PhysicalBoolean(SignalKey<Boolean> key, BooleanSupplier getter) {
            this.key = Objects.requireNonNull(key, "key");
            this.getter = Objects.requireNonNull(getter, "getter");
        }

        public SignalKey<Boolean> key() {
            return key;
        }

        public BooleanSupplier getter() {
            return getter;
        }
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
        private final List<NamedNumericBinding> namedNumerics = new ArrayList<>();
        private final List<NamedDigitalBinding> namedDigitals = new ArrayList<>();
        private final Set<String> extraNames = new HashSet<>();
        private final List<PhysicalDouble> physicalDoubles = new ArrayList<>();
        private final List<PhysicalBoolean> physicalBooleans = new ArrayList<>();
        private final InputRequirements requirements = InputRequirements.create();
        private SignalKey<Double> ticksKey;
        private SignalKey<Double> ticksPerSecondKey;
        private SignalKey<Double> currentKey;
        private SignalKey<Boolean> lowerLimitKey;
        private SignalKey<Boolean> upperLimitKey;
        private SignalKey<Double> absoluteKey;
        private SignalKey<Double> redundantKey;
        private boolean declared;
        private InputValues inputValues;

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
            if (ticks != null) {
                this.ticksKey = MimicSignals.position(mechanismId);
                requireDouble(ticksKey, ticks, InputPriority.NORMAL);
            }
            return this;
        }

        public Builder ticksPerSecond(DoubleSupplier ticksPerSecond) {
            this.ticksPerSecond = ticksPerSecond;
            if (ticksPerSecond != null) {
                this.ticksPerSecondKey = MimicSignals.velocity(mechanismId);
                requireDouble(ticksPerSecondKey, ticksPerSecond, InputPriority.NORMAL);
            }
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
            if (currentAmps != null) {
                this.currentKey = MimicSignals.current(mechanismId);
                requireDouble(currentKey, currentAmps, InputPriority.OPTIONAL);
            }
            return this;
        }

        public Builder lowerLimit(BooleanSupplier lowerLimitRaw, boolean inverted) {
            this.lowerLimitRaw = lowerLimitRaw;
            this.lowerLimitInverted = inverted;
            if (lowerLimitRaw != null) {
                this.lowerLimitKey = MimicSignals.lowerLimit(mechanismId);
                requireBoolean(lowerLimitKey, lowerLimitRaw, InputPriority.CRITICAL);
            }
            return this;
        }

        public Builder upperLimit(BooleanSupplier upperLimitRaw, boolean inverted) {
            this.upperLimitRaw = upperLimitRaw;
            this.upperLimitInverted = inverted;
            if (upperLimitRaw != null) {
                this.upperLimitKey = MimicSignals.upperLimit(mechanismId);
                requireBoolean(upperLimitKey, upperLimitRaw, InputPriority.CRITICAL);
            }
            return this;
        }

        public Builder absoluteSensor(DoubleSupplier absoluteSensor, String unitSymbol) {
            this.absoluteSensor = absoluteSensor;
            this.absoluteUnitSymbol = unitSymbol == null ? "" : unitSymbol;
            if (absoluteSensor != null) {
                this.absoluteKey = MimicSignals.absolute(mechanismId);
                requireDouble(absoluteKey, absoluteSensor, InputPriority.NORMAL);
            }
            return this;
        }

        public Builder redundantTicks(DoubleSupplier redundantTicks) {
            this.redundantTicks = redundantTicks;
            if (redundantTicks != null) {
                this.redundantKey = MimicSignals.redundant(mechanismId);
                requireDouble(redundantKey, redundantTicks, InputPriority.NORMAL);
            }
            return this;
        }

        /**
         * Optional numeric extra keyed by team-owned name and library role.
         * A null supplier is treated as unwired and is not added. Missing
         * lookups stay {@link MeasurementValidity#UNSUPPORTED}. Does not
         * write hardware.
         */
        public Builder namedSample(
                String name, SensorRole role, DoubleSupplier supplier, String unitSymbol) {
            if (supplier == null) {
                return this;
            }
            Objects.requireNonNull(role, "role");
            String extraName = requireExtraName(name);
            SignalKey<Double> key = MimicSignals.namedNumeric(mechanismId, extraName);
            requireDouble(key, supplier, InputPriority.NORMAL);
            namedNumerics.add(
                    new NamedNumericBinding(
                            extraName,
                            role,
                            supplier,
                            key,
                            unitSymbol == null ? role.unitSymbol() : unitSymbol));
            return this;
        }

        /**
         * Optional digital extra (piece entry, latch, home index, …). A null
         * supplier is treated as unwired and is not added. Missing lookups
         * stay {@link MeasurementValidity#UNSUPPORTED}, not a fake
         * {@code false}. Does not write hardware.
         */
        public Builder namedLimit(
                String name, SensorRole role, BooleanSupplier supplier, boolean inverted) {
            if (supplier == null) {
                return this;
            }
            Objects.requireNonNull(role, "role");
            String extraName = requireExtraName(name);
            SignalKey<Boolean> key = MimicSignals.namedDigital(mechanismId, extraName);
            requireBoolean(key, supplier, InputPriority.NORMAL);
            namedDigitals.add(new NamedDigitalBinding(extraName, role, supplier, key, inverted));
            return this;
        }

        /**
         * Register MIMIC sensor keys on a sampler. Call before freeze. Pair
         * with {@link #readFrom(InputValues)} so {@code capture()} does not
         * call hardware getters a second time. Requested/applied effort are
         * last-command values and are not registered.
         */
        public Builder declareInputs(InputRegistrar registrar) {
            requirements.registerWith(Objects.requireNonNull(registrar, "registrar"));
            declared = true;
            return this;
        }

        /**
         * Observe published samples instead of the physical suppliers. Must
         * follow {@link #declareInputs(InputRegistrar)}.
         */
        public Builder readFrom(InputValues values) {
            this.inputValues = Objects.requireNonNull(values, "values");
            return this;
        }

        public List<PhysicalDouble> physicalDoubles() {
            return Collections.unmodifiableList(physicalDoubles);
        }

        public List<PhysicalBoolean> physicalBooleans() {
            return Collections.unmodifiableList(physicalBooleans);
        }

        private String requireExtraName(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("named extra name must be non-empty");
            }
            if (!extraNames.add(name)) {
                throw new IllegalArgumentException("duplicate extra name " + name);
            }
            return name;
        }

        private void requireDouble(SignalKey<Double> key, DoubleSupplier getter, InputPriority priority) {
            physicalDoubles.add(new PhysicalDouble(key, getter));
            requirements.require(key, SamplingPolicy.everyCycle(), priority);
        }

        private void requireBoolean(SignalKey<Boolean> key, BooleanSupplier getter, InputPriority priority) {
            physicalBooleans.add(new PhysicalBoolean(key, getter));
            requirements.require(key, SamplingPolicy.everyCycle(), priority);
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
            if (inputValues != null && !declared) {
                throw new IllegalStateException("readFrom(InputValues) requires declareInputs(InputRegistrar) first");
            }
            if (declared && inputValues == null) {
                throw new IllegalStateException(
                        "declareInputs(...) requires readFrom(InputValues) so capture() does not call hardware a second time");
            }
            return new MechanismObserver(this);
        }
    }
}
