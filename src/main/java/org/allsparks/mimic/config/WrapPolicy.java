package org.allsparks.mimic.config;

/**
 * How soft bounds treat the pose coordinate. This is a declaration for later
 * limit checks. It does not unwrap encoders, plan shortest-path motion, or
 * write motors.
 *
 * {@link #NONE} is the linear interval {@code min <= pose <= max}. Use it for
 * slides and elevators.
 *
 * {@link #WRAP_AWARE} is an allowed arc on a circle of a declared period (for
 * example 360 degrees or 2π radians). Use it for rotary axes whose travel is
 * limited by a cable wrap or hard stop, not by a linear encoder range.
 *
 * Wrap-aware bounds are not "spin forever." The complementary arc is the
 * forbidden wrap stop. If {@code min <= max}, the allowed arc does not cross
 * 0. If {@code min > max}, the allowed arc crosses 0 (for example
 * {@code min = 350}, {@code max = 10} is the short window through 0). Pose is
 * reduced modulo the period before the inside-arc test. Stopping margin
 * shrinks that arc from both ends along the circle.
 */
public enum WrapPolicy {
    NONE,
    WRAP_AWARE
}
