package org.allsparks.mimic.config;

/**
 * Declared calibration strategy. Phase 0 never homes. Hard-stop current
 * detection is permitted only when a later phase explicitly enables it.
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
