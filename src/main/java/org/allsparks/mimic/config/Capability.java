package org.allsparks.mimic.config;

/**
 * Declared reusable behavior. Enabling a capability does not implement it and
 * does not write hardware.
 */
public enum Capability {
    HOMING,
    SOFT_LIMITS,
    HARD_LIMITS,
    HOLD_POSITION,
    MULTI_ACTUATOR_SYNCHRONIZATION,
    READY_AT_SPEED,
    PIECE_TRACKING,
    PIECE_COUNTING,
    JAM_DETECTION,
    STALL_DETECTION,
    GRAVITY_COMPENSATION,
    PROFILED_MOTION,
    NAMED_STATES,
    WRAP_AWARE_ROTATION
}
