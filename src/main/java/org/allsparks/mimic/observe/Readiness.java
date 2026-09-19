package org.allsparks.mimic.observe;

import java.util.Objects;

/**
 * Pure at-speed / in-tolerance evaluation with dwell and hysteresis.
 *
 * One valid loop inside the band is not ready: the dwell window must elapse
 * on consecutive {@link SensorSample#isUsable() usable} samples. Invalid,
 * {@link MeasurementValidity#MISSING}, {@link MeasurementValidity#UNSUPPORTED},
 * or {@link MeasurementValidity#STALE} measurements are not ready and reset
 * the window. After ready, a small dip stays ready until the value crosses
 * the hysteresis edge.
 *
 * Immutable Java 11 state: {@link #feed(MechanismSnapshot)} returns a new
 * evaluator. Feeding snapshots does not write motors or servos. This is not
 * a feeder interlock and does not fire a launcher.
 */
public final class Readiness {
    private enum Kind {
        AT_SPEED,
        IN_TOLERANCE
    }

    private final Kind kind;
    private final double minVel;
    private final double target;
    private final double tolerance;
    private final double hysteresis;
    private final long dwellNanos;
    private final boolean inBand;
    private final Long windowStartNanos;
    private final boolean ready;

    private Readiness(
            Kind kind,
            double minVel,
            double target,
            double tolerance,
            double hysteresis,
            long dwellNanos,
            boolean inBand,
            Long windowStartNanos,
            boolean ready) {
        this.kind = kind;
        this.minVel = minVel;
        this.target = target;
        this.tolerance = tolerance;
        this.hysteresis = hysteresis;
        this.dwellNanos = dwellNanos;
        this.inBand = inBand;
        this.windowStartNanos = windowStartNanos;
        this.ready = ready;
    }

    /**
     * At-speed evaluator with no samples yet. Not ready until
     * {@link #feed(MechanismSnapshot)} sees a usable velocity at or above
     * {@code minVel} for {@code dwellNanos}.
     */
    public static Readiness atSpeed(double minVel, double hysteresis, long dwellNanos) {
        requireFiniteNonNegative("minVel", minVel);
        requireFiniteNonNegative("hysteresis", hysteresis);
        requireNonNegativeDwell(dwellNanos);
        return new Readiness(Kind.AT_SPEED, minVel, 0.0, 0.0, hysteresis, dwellNanos, false, null, false);
    }

    /**
     * Evaluate at-speed from this snapshot. Sketch:
     * {@code Readiness.atSpeed(snapshot, minVel, hysteresis, dwellNanos)}.
     */
    public static Readiness atSpeed(
            MechanismSnapshot snapshot, double minVel, double hysteresis, long dwellNanos) {
        return atSpeed(minVel, hysteresis, dwellNanos).feed(snapshot);
    }

    /**
     * Position-in-tolerance evaluator with no samples yet. Not ready until
     * {@link #feed(MechanismSnapshot)} sees a usable position within
     * {@code tolerance} of {@code target} for {@code dwellNanos}.
     */
    public static Readiness inTolerance(
            double target, double tolerance, double hysteresis, long dwellNanos) {
        requireFinite("target", target);
        requireFiniteNonNegative("tolerance", tolerance);
        requireFiniteNonNegative("hysteresis", hysteresis);
        requireNonNegativeDwell(dwellNanos);
        return new Readiness(
                Kind.IN_TOLERANCE, 0.0, target, tolerance, hysteresis, dwellNanos, false, null, false);
    }

    /**
     * Evaluate in-tolerance from this snapshot. One loop inside the band is
     * not settled when {@code dwellNanos} is greater than zero.
     */
    public static Readiness inTolerance(
            MechanismSnapshot snapshot,
            double target,
            double tolerance,
            double hysteresis,
            long dwellNanos) {
        return inTolerance(target, tolerance, hysteresis, dwellNanos).feed(snapshot);
    }

    /**
     * Fold the next snapshot into a new evaluator. Reads samples only; does
     * not write hardware.
     */
    public Readiness feed(MechanismSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        SensorSample sample = kind == Kind.AT_SPEED ? snapshot.velocitySample() : snapshot.positionSample();
        if (!sampleUsable(sample)) {
            return reset();
        }
        boolean stillInBand = qualifies(sample.value(), inBand);
        if (!stillInBand) {
            return reset();
        }
        long time = snapshot.timestampNanos();
        long start = windowStartNanos == null || time < windowStartNanos ? time : windowStartNanos;
        boolean nowReady = time - start >= dwellNanos;
        return new Readiness(
                kind, minVel, target, tolerance, hysteresis, dwellNanos, true, start, nowReady);
    }

    /**
     * True only after a usable in-band window of {@link #dwellNanos()} has
     * elapsed. False on the first qualifying loop when dwell is positive.
     */
    public boolean ready() {
        return ready;
    }

    /**
     * True while the latest usable sample is inside the enter or hysteresis
     * stay band. In-band is not ready.
     */
    public boolean inBand() {
        return inBand;
    }

    public double minVel() {
        return minVel;
    }

    public double target() {
        return target;
    }

    public double tolerance() {
        return tolerance;
    }

    public double hysteresis() {
        return hysteresis;
    }

    public long dwellNanos() {
        return dwellNanos;
    }

    private Readiness reset() {
        return new Readiness(
                kind, minVel, target, tolerance, hysteresis, dwellNanos, false, null, false);
    }

    private boolean qualifies(double value, boolean currentlyInBand) {
        if (kind == Kind.AT_SPEED) {
            double stay = minVel - hysteresis;
            return currentlyInBand ? value >= stay : value >= minVel;
        }
        double error = Math.abs(value - target);
        double stay = tolerance + hysteresis;
        return currentlyInBand ? error <= stay : error <= tolerance;
    }

    private static boolean sampleUsable(SensorSample sample) {
        return sample.isUsable() && Double.isFinite(sample.value());
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }

    private static void requireNonNegativeDwell(long dwellNanos) {
        if (dwellNanos < 0L) {
            throw new IllegalArgumentException("dwellNanos must be >= 0");
        }
    }
}
