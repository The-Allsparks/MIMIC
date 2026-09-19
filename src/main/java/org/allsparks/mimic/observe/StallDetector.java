package org.allsparks.mimic.observe;

import java.util.Objects;

/**
 * Observe-only stall detector. Sketch:
 * {@code StallDetector.update(snapshot) -> suspected / not}.
 *
 * A stall is high current plus no motion for a timeout, not a single
 * current sample. Timeout is required. Missing, NaN, or unwired current
 * is {@link StallSuspicion#UNSUPPORTED}, not stalled. Current-only is not
 * a hard limit: velocity must also show no motion, and this type does not
 * write hardware.
 *
 * Call {@link #update(MechanismSnapshot)} once per loop on the same
 * instance so the timeout window can elapse. Unused by {@code MimicSession}.
 * Reverse-clear and bounded jam clearing stay forbidden.
 */
public final class StallDetector {
    private final double currentThresholdAmps;
    private final double motionEpsilon;
    private final long timeoutNanos;
    private Long windowStartNanos;

    private StallDetector(double currentThresholdAmps, double motionEpsilon, long timeoutNanos) {
        this.currentThresholdAmps = currentThresholdAmps;
        this.motionEpsilon = motionEpsilon;
        this.timeoutNanos = timeoutNanos;
    }

    /**
     * Detector with no samples yet. Not suspected until
     * {@link #update(MechanismSnapshot)} sees usable high current and no
     * motion for {@code timeoutNanos}.
     *
     * @param currentThresholdAmps finite amps that count as high current; must be {@code > 0}
     * @param motionEpsilon largest absolute velocity that still counts as no motion; must be {@code >= 0}
     * @param timeoutNanos required window; must be {@code > 0}
     */
    public static StallDetector of(
            double currentThresholdAmps, double motionEpsilon, long timeoutNanos) {
        requireFinitePositive("currentThresholdAmps", currentThresholdAmps);
        requireFiniteNonNegative("motionEpsilon", motionEpsilon);
        if (timeoutNanos <= 0L) {
            throw new IllegalArgumentException("timeoutNanos is required and must be > 0");
        }
        return new StallDetector(currentThresholdAmps, motionEpsilon, timeoutNanos);
    }

    /**
     * Fold the next snapshot. Reads current and velocity only; does not
     * write hardware. One qualifying loop is not suspected.
     */
    public StallSuspicion update(MechanismSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!currentUsable(snapshot.currentAmps())) {
            windowStartNanos = null;
            return StallSuspicion.UNSUPPORTED;
        }
        SensorSample velocity = snapshot.velocitySample();
        if (!velocityUsable(velocity)) {
            windowStartNanos = null;
            return StallSuspicion.UNSUPPORTED;
        }
        boolean highCurrent = Math.abs(snapshot.currentAmps()) >= currentThresholdAmps;
        boolean noMotion = Math.abs(velocity.value()) <= motionEpsilon;
        if (!highCurrent || !noMotion) {
            windowStartNanos = null;
            return StallSuspicion.NOT_SUSPECTED;
        }
        long time = snapshot.timestampNanos();
        long start = windowStartNanos == null || time < windowStartNanos ? time : windowStartNanos;
        windowStartNanos = start;
        if (time - start >= timeoutNanos) {
            return StallSuspicion.SUSPECTED;
        }
        return StallSuspicion.NOT_SUSPECTED;
    }

    public double currentThresholdAmps() {
        return currentThresholdAmps;
    }

    public double motionEpsilon() {
        return motionEpsilon;
    }

    public long timeoutNanos() {
        return timeoutNanos;
    }

    private static boolean currentUsable(double amps) {
        return Double.isFinite(amps);
    }

    private static boolean velocityUsable(SensorSample sample) {
        return sample.isUsable() && Double.isFinite(sample.value());
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and > 0");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }
}
