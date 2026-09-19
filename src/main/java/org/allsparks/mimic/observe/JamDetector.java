package org.allsparks.mimic.observe;

/**
 * Observe-only jam detector. Same current-plus-no-motion-for-timeout
 * heuristic as {@link StallDetector}. Reverse-clear is forbidden: this
 * type never calls {@code setPower} and is unused by {@code MimicSession}.
 */
public final class JamDetector {
    private final StallDetector stall;

    private JamDetector(StallDetector stall) {
        this.stall = stall;
    }

    /**
     * Detector with no samples yet. Timeout is required.
     *
     * @param currentThresholdAmps finite amps that count as high current; must be {@code > 0}
     * @param motionEpsilon largest absolute velocity that still counts as no motion; must be {@code >= 0}
     * @param timeoutNanos required window; must be {@code > 0}
     */
    public static JamDetector of(
            double currentThresholdAmps, double motionEpsilon, long timeoutNanos) {
        return new JamDetector(StallDetector.of(currentThresholdAmps, motionEpsilon, timeoutNanos));
    }

    /**
     * Fold the next snapshot. Sketch matches stall:
     * {@code update(snapshot) -> suspected / not}. Does not write hardware.
     */
    public JamSuspicion update(MechanismSnapshot snapshot) {
        return JamSuspicion.from(stall.update(snapshot));
    }
}
