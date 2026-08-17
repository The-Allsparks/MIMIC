package org.allsparks.mimic.api;

/**
 * Calibration lifecycle for a mechanism.
 *
 * Encoder ticks are not a physical pose until a homing or absolute-sensor
 * strategy has completed. Phase 0 reports {@link #UNCALIBRATED} unless a
 * caller supplies an observed state; it never homes hardware.
 */
public enum CalibrationState {
    UNCALIBRATED,
    HOMING,
    CALIBRATED,
    CALIBRATION_SUSPECT,
    FAULTED
}
