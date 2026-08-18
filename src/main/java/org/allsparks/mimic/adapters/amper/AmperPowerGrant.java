package org.allsparks.mimic.adapters.amper;

/**
 * Constraint returned by AMPER. Until Phase 9 is enabled and validated,
 * callers must treat grants as advisory telemetry only.
 *
 * AMPER must not command mechanism hardware. MIMIC must still apply limits
 * and interlocks after a grant.
 */
public final class AmperPowerGrant {
    private final double allowedEffort;
    private final boolean delayed;
    private final String reason;
    private final double confidence;

    public AmperPowerGrant(double allowedEffort, boolean delayed, String reason, double confidence) {
        if (reason == null || reason.isEmpty()) {
            throw new IllegalArgumentException("reason must be non-empty");
        }
        if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be in [0, 1]");
        }
        this.allowedEffort = allowedEffort;
        this.delayed = delayed;
        this.reason = reason;
        this.confidence = confidence;
    }

    public static AmperPowerGrant unrestricted(double requestedEffort) {
        return new AmperPowerGrant(requestedEffort, false, "FEATURE_DISABLED", 1.0);
    }

    public double allowedEffort() {
        return allowedEffort;
    }

    public boolean delayed() {
        return delayed;
    }

    public String reason() {
        return reason;
    }

    public double confidence() {
        return confidence;
    }
}
