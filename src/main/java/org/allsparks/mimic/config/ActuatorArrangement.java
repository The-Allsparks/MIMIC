package org.allsparks.mimic.config;

/**
 * How actuators are arranged. Independent of construct. Count greater than one
 * does not imply {@link Capability#MULTI_ACTUATOR_SYNCHRONIZATION}.
 */
public enum ActuatorArrangement {
    NONE,
    POSITIONAL_SERVO,
    CONTINUOUS_ROTATION_SERVO,
    SINGLE_MOTOR,
    MECHANICALLY_LINKED_MOTORS,
    INDEPENDENTLY_SENSED_MOTORS,
    OPPOSED_MOTORS,
    MOTOR_PLUS_SERVO_RELEASE,
    MOTOR_PLUS_BRAKE,
    MOTOR_PLUS_RATCHET
}
