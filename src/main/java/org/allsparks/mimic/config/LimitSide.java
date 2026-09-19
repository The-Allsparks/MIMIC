package org.allsparks.mimic.config;

/**
 * Which declared bound a later command is traveling into. {@link #MIN} is
 * decreasing pose (retract / clockwise-stop, depending on units).
 * {@link #MAX} is increasing pose. This is not a motor direction enum and
 * does not write hardware.
 */
public enum LimitSide {
    MIN,
    MAX
}
