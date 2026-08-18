package org.allsparks.mimic.adapters.amper;

/**
 * Future AMPER request fields owned by MIMIC. AMPER allocates power; MIMIC
 * retains mechanism safety.
 *
 * Phase 0–1 must not use grants to change motor output. This type exists so
 * the integration contract can be reviewed before Phase 9.
 */
public final class AmperPowerRequest {
    private final String source;
    private final double requestedEffort;
    private final double estimatedCurrentAmps;
    private final double safeMinimumHoldingEffort;
    private final boolean gravityCritical;
    private final boolean interruptible;
    private final long acceptableDelayNanos;
    private final long expectedDurationNanos;
    private final String motionPhase;

    public AmperPowerRequest(
            String source,
            double requestedEffort,
            double estimatedCurrentAmps,
            double safeMinimumHoldingEffort,
            boolean gravityCritical,
            boolean interruptible,
            long acceptableDelayNanos,
            long expectedDurationNanos,
            String motionPhase) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("source must be non-empty");
        }
        this.source = source;
        this.requestedEffort = requestedEffort;
        this.estimatedCurrentAmps = estimatedCurrentAmps;
        this.safeMinimumHoldingEffort = safeMinimumHoldingEffort;
        this.gravityCritical = gravityCritical;
        this.interruptible = interruptible;
        this.acceptableDelayNanos = acceptableDelayNanos;
        this.expectedDurationNanos = expectedDurationNanos;
        this.motionPhase = motionPhase == null ? "" : motionPhase;
    }

    public String source() {
        return source;
    }

    public double requestedEffort() {
        return requestedEffort;
    }

    public double estimatedCurrentAmps() {
        return estimatedCurrentAmps;
    }

    public double safeMinimumHoldingEffort() {
        return safeMinimumHoldingEffort;
    }

    public boolean gravityCritical() {
        return gravityCritical;
    }

    public boolean interruptible() {
        return interruptible;
    }

    public long acceptableDelayNanos() {
        return acceptableDelayNanos;
    }

    public long expectedDurationNanos() {
        return expectedDurationNanos;
    }

    public String motionPhase() {
        return motionPhase;
    }
}
