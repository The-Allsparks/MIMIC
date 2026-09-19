package org.allsparks.mimic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActuatorTopologyTest {

    @Test
    void factoryMethodsPreserveArrangementAndCount() {
        assertEquals(ActuatorArrangement.NONE, ActuatorTopology.none().arrangement());
        assertEquals(0, ActuatorTopology.none().actuatorCount());
        assertTrue(ActuatorTopology.none().isPassive());

        assertEquals(ActuatorArrangement.POSITIONAL_SERVO, ActuatorTopology.positionalServo().arrangement());
        assertEquals(1, ActuatorTopology.positionalServo().actuatorCount());
        assertTrue(ActuatorTopology.positionalServo().isPositionalServo());

        assertEquals(
                ActuatorArrangement.CONTINUOUS_ROTATION_SERVO,
                ActuatorTopology.continuousRotationServo().arrangement());
        assertEquals(1, ActuatorTopology.continuousRotationServo().actuatorCount());

        assertEquals(ActuatorArrangement.SINGLE_MOTOR, ActuatorTopology.singleMotor().arrangement());
        assertEquals(1, ActuatorTopology.singleMotor().actuatorCount());

        assertEquals(ActuatorArrangement.MOTOR_PLUS_SERVO_RELEASE, ActuatorTopology.motorPlusServoRelease().arrangement());
        assertEquals(2, ActuatorTopology.motorPlusServoRelease().actuatorCount());
        assertEquals(ActuatorArrangement.MOTOR_PLUS_BRAKE, ActuatorTopology.motorPlusBrake().arrangement());
        assertEquals(2, ActuatorTopology.motorPlusBrake().actuatorCount());
        assertEquals(ActuatorArrangement.MOTOR_PLUS_RATCHET, ActuatorTopology.motorPlusRatchet().arrangement());
        assertEquals(2, ActuatorTopology.motorPlusRatchet().actuatorCount());
    }

    @Test
    void linkedAndIndependentAreDistinct() {
        ActuatorTopology linked = ActuatorTopology.mechanicallyLinkedMotors(2);
        ActuatorTopology independent = ActuatorTopology.independentlySensedMotors(2);
        ActuatorTopology opposed = ActuatorTopology.opposedMotors(2);

        assertTrue(linked.isMechanicallyLinked());
        assertFalse(linked.isIndependentlySensedMotors());
        assertEquals(ActuatorArrangement.MECHANICALLY_LINKED_MOTORS, linked.arrangement());
        assertEquals(2, linked.actuatorCount());

        assertTrue(independent.isIndependentlySensedMotors());
        assertFalse(independent.isMechanicallyLinked());
        assertEquals(ActuatorArrangement.INDEPENDENTLY_SENSED_MOTORS, independent.arrangement());
        assertEquals(2, independent.actuatorCount());

        assertFalse(opposed.isMechanicallyLinked());
        assertFalse(opposed.isIndependentlySensedMotors());
        assertEquals(ActuatorArrangement.OPPOSED_MOTORS, opposed.arrangement());

        assertNotEquals(linked, independent);
        assertNotEquals(linked, opposed);
        assertEquals(linked, ActuatorTopology.mechanicallyLinkedMotors(2));
        assertEquals(independent, ActuatorTopology.independentlySensedMotors(2));
    }

    @Test
    void actuatorCountGreaterThanOneDoesNotImplySynchronization() {
        ActuatorTopology[] topologies = {
            ActuatorTopology.none(),
            ActuatorTopology.positionalServo(),
            ActuatorTopology.continuousRotationServo(),
            ActuatorTopology.singleMotor(),
            ActuatorTopology.mechanicallyLinkedMotors(2),
            ActuatorTopology.mechanicallyLinkedMotors(3),
            ActuatorTopology.independentlySensedMotors(2),
            ActuatorTopology.independentlySensedMotors(4),
            ActuatorTopology.opposedMotors(2),
            ActuatorTopology.motorPlusServoRelease(),
            ActuatorTopology.motorPlusBrake(),
            ActuatorTopology.motorPlusRatchet()
        };
        for (ActuatorTopology topology : topologies) {
            assertFalse(
                    topology.impliesIndependentSynchronization(),
                    topology + " must not imply independent synchronization");
        }
    }

    @Test
    void namedFactoriesRejectTooFewActuators() {
        assertThrows(IllegalArgumentException.class, () -> ActuatorTopology.mechanicallyLinkedMotors(1));
        assertThrows(IllegalArgumentException.class, () -> ActuatorTopology.independentlySensedMotors(1));
        assertThrows(IllegalArgumentException.class, () -> ActuatorTopology.opposedMotors(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> ActuatorTopology.of(ActuatorArrangement.SINGLE_MOTOR, -1));
    }
}
