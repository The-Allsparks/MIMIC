package org.allsparks.mimic.config;

/**
 * Declared response when an optional sensor is missing or invalid. Phase 0
 * does not actuate; {@link #STOP_MECHANISM} is a declaration for later phases.
 */
public enum DegradedBehavior {
    IGNORE_OPTIONAL,
    MARK_DEGRADED,
    DISABLE_CAPABILITY,
    STOP_MECHANISM
}
