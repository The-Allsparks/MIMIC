package org.allsparks.mimic.config;

/**
 * Declared calibration strategy. Phase 0 never homes. Optional
 * {@link CalibrationContract} bounds do not run homing. Hard-stop current
 * detection stays blocked: declaring a contract must not enable it.
 */
public enum CalibrationStrategy {
    NONE,
    KNOWN_STARTUP_POSE,
    ABSOLUTE_SENSOR,
    HOME_SWITCH,
    HARD_STOP_CURRENT,
    INDEX_PULSE,
    MANUAL,
    RETAINED_WITH_VALIDATION
}
