package org.allsparks.mimic.config;

import java.util.Objects;

/**
 * Immutable actuator layout for one mechanism instance.
 *
 * Mechanically linked motors share a command. Independently sensed motors may
 * enable synchronization as an explicit capability. This object never writes
 * hardware.
 */
public final class ActuatorTopology {
    private final ActuatorArrangement arrangement;
    private final int actuatorCount;

    private ActuatorTopology(ActuatorArrangement arrangement, int actuatorCount) {
        this.arrangement = Objects.requireNonNull(arrangement, "arrangement");
        if (actuatorCount < 0) {
            throw new IllegalArgumentException("actuatorCount must be >= 0");
        }
        this.actuatorCount = actuatorCount;
    }

    public static ActuatorTopology none() {
        return new ActuatorTopology(ActuatorArrangement.NONE, 0);
    }

    public static ActuatorTopology positionalServo() {
        return new ActuatorTopology(ActuatorArrangement.POSITIONAL_SERVO, 1);
    }

    public static ActuatorTopology continuousRotationServo() {
        return new ActuatorTopology(ActuatorArrangement.CONTINUOUS_ROTATION_SERVO, 1);
    }

    public static ActuatorTopology singleMotor() {
        return new ActuatorTopology(ActuatorArrangement.SINGLE_MOTOR, 1);
    }

    public static ActuatorTopology mechanicallyLinkedMotors(int count) {
        requireAtLeast(count, 2, "mechanically linked motors");
        return new ActuatorTopology(ActuatorArrangement.MECHANICALLY_LINKED_MOTORS, count);
    }

    public static ActuatorTopology independentlySensedMotors(int count) {
        requireAtLeast(count, 2, "independently sensed motors");
        return new ActuatorTopology(ActuatorArrangement.INDEPENDENTLY_SENSED_MOTORS, count);
    }

    public static ActuatorTopology opposedMotors(int count) {
        requireAtLeast(count, 2, "opposed motors");
        return new ActuatorTopology(ActuatorArrangement.OPPOSED_MOTORS, count);
    }

    public static ActuatorTopology motorPlusServoRelease() {
        return new ActuatorTopology(ActuatorArrangement.MOTOR_PLUS_SERVO_RELEASE, 2);
    }

    public static ActuatorTopology motorPlusBrake() {
        return new ActuatorTopology(ActuatorArrangement.MOTOR_PLUS_BRAKE, 2);
    }

    public static ActuatorTopology motorPlusRatchet() {
        return new ActuatorTopology(ActuatorArrangement.MOTOR_PLUS_RATCHET, 2);
    }

    public static ActuatorTopology of(ActuatorArrangement arrangement, int actuatorCount) {
        return new ActuatorTopology(arrangement, actuatorCount);
    }

    public ActuatorArrangement arrangement() {
        return arrangement;
    }

    public int actuatorCount() {
        return actuatorCount;
    }

    public boolean isPassive() {
        return arrangement == ActuatorArrangement.NONE || actuatorCount == 0;
    }

    public boolean isPositionalServo() {
        return arrangement == ActuatorArrangement.POSITIONAL_SERVO;
    }

    public boolean isMechanicallyLinked() {
        return arrangement == ActuatorArrangement.MECHANICALLY_LINKED_MOTORS;
    }

    public boolean isIndependentlySensedMotors() {
        return arrangement == ActuatorArrangement.INDEPENDENTLY_SENSED_MOTORS;
    }

    public boolean impliesIndependentSynchronization() {
        return false;
    }

    private static void requireAtLeast(int count, int minimum, String label) {
        if (count < minimum) {
            throw new IllegalArgumentException(label + " requires at least " + minimum + " actuators");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ActuatorTopology)) {
            return false;
        }
        ActuatorTopology that = (ActuatorTopology) other;
        return actuatorCount == that.actuatorCount && arrangement == that.arrangement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrangement, actuatorCount);
    }

    @Override
    public String toString() {
        return arrangement.name() + "(" + actuatorCount + ")";
    }
}
