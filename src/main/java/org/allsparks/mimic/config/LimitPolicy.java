package org.allsparks.mimic.config;

/**
 * Declared limit policy. Not an actuator safety gate. Missing switches must
 * not be treated as "not asserted" when a later phase enforces limits.
 */
public enum LimitPolicy {
    NONE,
    SOFT_ONLY,
    HARD_ONLY,
    SOFT_AND_HARD
}
