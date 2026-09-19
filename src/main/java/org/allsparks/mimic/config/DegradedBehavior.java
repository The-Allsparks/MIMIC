package org.allsparks.mimic.config;

/**
 * Declared response when an optional sensor is missing or invalid. Phase 0
 * does not actuate; {@link #STOP_MECHANISM} is a declaration for later phases.
 * {@link FaultPolicy} maps this plus a {@link FaultKind} onto
 * {@link FaultSeverity}. Required sensors cannot use {@link #IGNORE_OPTIONAL}
 * ({@link ConfigurationValidator#REQUIRED_SENSOR_IGNORE_OPTIONAL}).
 */
public enum DegradedBehavior {
    IGNORE_OPTIONAL,
    MARK_DEGRADED,
    DISABLE_CAPABILITY,
    STOP_MECHANISM
}
