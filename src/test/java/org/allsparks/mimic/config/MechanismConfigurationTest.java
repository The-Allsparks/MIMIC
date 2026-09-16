package org.allsparks.mimic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.templates.MechanismFamily;
import org.allsparks.mimic.templates.MechanismMotionKind;
import org.junit.jupiter.api.Test;

class MechanismConfigurationTest {

    @Test
    void everyStandardPresetPassesValidation() {
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            MechanismConfiguration configuration =
                    StandardPresets.exampleConfiguration("example-" + construct.name(), construct);
            assertTrue(configuration.validate().valid(), construct.name());
            assertEquals(construct.family(), configuration.family(), construct.name());
        }
    }

    @Test
    void customConstructDoesNotRequireEnumEdit() {
        ConstructDescriptor custom =
                ConstructDescriptor.custom(
                        "sideRoller",
                        MechanismFamily.INTAKE,
                        MechanismMotionKind.CONTINUOUS,
                        "Team-specific roller layout");
        MechanismConfiguration configuration =
                MechanismConfiguration.builder("sideIntake")
                        .construct(custom)
                        .actuators(ActuatorTopology.singleMotor())
                        .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                        .build();
        assertFalse(custom.isStandard());
        assertEquals("sideRoller", configuration.construct().id());
        assertFalse(configuration.toBlueprint().isPresent());
    }

    @Test
    void configurationIsImmutable() {
        MechanismConfiguration configuration =
                MechanismConfiguration.builder("mainLift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.independentlySensedMotors(2))
                        .sensor("leftPosition", SensorRole.RELATIVE_POSITION)
                        .sensor("rightPosition", SensorRole.REDUNDANT_POSITION)
                        .sensor("home", SensorRole.RETRACT_LIMIT)
                        .enable(Capability.HOMING)
                        .enable(Capability.SOFT_LIMITS)
                        .enable(Capability.MULTI_ACTUATOR_SYNCHRONIZATION)
                        .enable(Capability.HOLD_POSITION)
                        .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .degradedBehavior(SensorRole.REDUNDANT_POSITION, DegradedBehavior.STOP_MECHANISM)
                        .build();
        assertThrows(UnsupportedOperationException.class, () -> configuration.sensors().add(null));
        assertThrows(UnsupportedOperationException.class, () -> configuration.capabilities().add(Capability.JAM_DETECTION));
        assertEquals("mainLift", configuration.mechanismId());
        assertEquals(DegradedBehavior.STOP_MECHANISM, configuration.degradedBehavior(SensorRole.REDUNDANT_POSITION));
        assertTrue(configuration.toBlueprint().isPresent());
        assertEquals(MechanismConstruct.ELEVATOR, configuration.toBlueprint().get().construct());
        assertTrue(configuration.namedStates().isEmpty());
    }

    @Test
    void namedStatesAreDeclaredImmutableAndNotAScheduler() {
        String[] names = {"OPEN", "CLOSED", "HOLDING"};
        MechanismConfiguration claw =
                MechanismConfiguration.builder("claw")
                        .construct(MechanismConstruct.CLAW)
                        .actuators(ActuatorTopology.positionalServo())
                        .enable(Capability.NAMED_STATES)
                        .controlDomain(ControlDomain.NAMED_STATE)
                        .namedStates(names)
                        .build();
        names[0] = "MUTATED";
        assertEquals(3, claw.namedStates().size());
        assertEquals("OPEN", claw.namedStates().get(0));
        assertEquals("CLOSED", claw.namedStates().get(1));
        assertEquals("HOLDING", claw.namedStates().get(2));
        assertThrows(UnsupportedOperationException.class, () -> claw.namedStates().add("FIRING"));
        assertThrows(UnsupportedOperationException.class, () -> claw.namedStates().clear());
        assertEquals(String.class, claw.namedStates().get(0).getClass());
    }

    @Test
    void emptyAndDuplicateNamedStatesAreRejected() {
        InvalidMechanismConfigurationException empty =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("claw")
                                .construct(MechanismConstruct.CLAW)
                                .actuators(ActuatorTopology.positionalServo())
                                .enable(Capability.NAMED_STATES)
                                .controlDomain(ControlDomain.NAMED_STATE)
                                .namedStates("OPEN", "", "CLOSED")
                                .build());
        assertTrue(empty.result().hasCode(ConfigurationValidator.EMPTY_NAMED_STATE));

        InvalidMechanismConfigurationException blank =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("claw")
                                .construct(MechanismConstruct.CLAW)
                                .actuators(ActuatorTopology.positionalServo())
                                .enable(Capability.NAMED_STATES)
                                .controlDomain(ControlDomain.NAMED_STATE)
                                .namedStates("OPEN", "   ")
                                .build());
        assertTrue(blank.result().hasCode(ConfigurationValidator.EMPTY_NAMED_STATE));

        InvalidMechanismConfigurationException duplicate =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("claw")
                                .construct(MechanismConstruct.CLAW)
                                .actuators(ActuatorTopology.positionalServo())
                                .enable(Capability.NAMED_STATES)
                                .controlDomain(ControlDomain.NAMED_STATE)
                                .namedStates("OPEN", "CLOSED", "OPEN")
                                .build());
        assertTrue(duplicate.result().hasCode(ConfigurationValidator.DUPLICATE_NAMED_STATE));

        ValidationResult reported =
                MechanismConfiguration.builder("claw")
                        .construct(MechanismConstruct.CLAW)
                        .actuators(ActuatorTopology.positionalServo())
                        .namedStates(null, "CLOSED")
                        .validate();
        assertFalse(reported.valid());
        assertTrue(reported.hasCode(ConfigurationValidator.EMPTY_NAMED_STATE));
    }

    @Test
    void nullAndEmptyIdsAreRejected() {
        assertThrows(
                InvalidMechanismConfigurationException.class,
                () -> MechanismConfiguration.builder(null)
                        .construct(MechanismConstruct.ROLLER_INTAKE)
                        .actuators(ActuatorTopology.singleMotor())
                        .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                        .build());
        assertThrows(
                InvalidMechanismConfigurationException.class,
                () -> MechanismConfiguration.builder("")
                        .construct(MechanismConstruct.ROLLER_INTAKE)
                        .actuators(ActuatorTopology.singleMotor())
                        .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                        .build());
        assertThrows(IllegalArgumentException.class, () -> ConstructDescriptor.custom("", MechanismFamily.INTAKE, MechanismMotionKind.CONTINUOUS, "x"));
    }

    @Test
    void homingRequiresAReference() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.singleMotor())
                                .enable(Capability.HOMING)
                                .calibrationStrategy(CalibrationStrategy.NONE)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.HOMING_WITHOUT_REFERENCE));
    }

    @Test
    void softLimitsRequirePositionAndCalibration() {
        InvalidMechanismConfigurationException noPosition =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.singleMotor())
                                .enable(Capability.SOFT_LIMITS)
                                .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(noPosition.result().hasCode(ConfigurationValidator.SOFT_LIMITS_WITHOUT_POSITION));

        InvalidMechanismConfigurationException noCalibration =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.singleMotor())
                                .sensor("position", SensorRole.RELATIVE_POSITION)
                                .enable(Capability.SOFT_LIMITS)
                                .calibrationStrategy(CalibrationStrategy.NONE)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(noCalibration.result().hasCode(ConfigurationValidator.SOFT_LIMITS_WITHOUT_CALIBRATION));
    }

    @Test
    void readyAtSpeedRequiresVelocity() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("flywheel")
                                .construct(MechanismConstruct.FLYWHEEL)
                                .actuators(ActuatorTopology.singleMotor())
                                .enable(Capability.READY_AT_SPEED)
                                .controlDomain(ControlDomain.VELOCITY)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.READY_AT_SPEED_WITHOUT_VELOCITY));
    }

    @Test
    void independentlySynchronizedActuatorsNeedEnoughFeedback() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.independentlySensedMotors(2))
                                .sensor("left", SensorRole.RELATIVE_POSITION)
                                .enable(Capability.MULTI_ACTUATOR_SYNCHRONIZATION)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.SYNC_INSUFFICIENT_FEEDBACK));
    }

    @Test
    void multipleActuatorsDoNotImplySynchronization() {
        MechanismConfiguration linked =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.mechanicallyLinkedMotors(2))
                        .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                        .build();
        assertFalse(linked.capabilities().contains(Capability.MULTI_ACTUATOR_SYNCHRONIZATION));
        assertFalse(linked.actuators().impliesIndependentSynchronization());
        assertEquals(2, linked.actuators().actuatorCount());
        assertFalse(linked.syncContract().isPresent());

        InvalidMechanismConfigurationException linkedSync =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.mechanicallyLinkedMotors(2))
                                .sensor("left", SensorRole.RELATIVE_POSITION)
                                .sensor("right", SensorRole.REDUNDANT_POSITION)
                                .enable(Capability.MULTI_ACTUATOR_SYNCHRONIZATION)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(linkedSync.result().hasCode(ConfigurationValidator.LINKED_MOTORS_WITH_INDEPENDENT_SYNC));
    }

    @Test
    void binaryServoDoesNotClaimMeasuredPosition() {
        MechanismConfiguration claw =
                MechanismConfiguration.builder("claw")
                        .construct(MechanismConstruct.CLAW)
                        .actuators(ActuatorTopology.positionalServo())
                        .enable(Capability.NAMED_STATES)
                        .controlDomain(ControlDomain.NAMED_STATE)
                        .build();
        for (SensorDeclaration sensor : claw.sensors()) {
            assertNotEquals(SensorRole.RELATIVE_POSITION, sensor.role());
        }

        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("claw")
                                .construct(MechanismConstruct.CLAW)
                                .actuators(ActuatorTopology.positionalServo())
                                .sensor("pretendPose", SensorRole.RELATIVE_POSITION)
                                .enable(Capability.NAMED_STATES)
                                .controlDomain(ControlDomain.NAMED_STATE)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.SERVO_COMMAND_AS_MEASURED_POSITION));
    }

    @Test
    void pieceCountingRequiresPassageEvidence() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("belt")
                                .construct(MechanismConstruct.BELT)
                                .actuators(ActuatorTopology.singleMotor())
                                .enable(Capability.PIECE_COUNTING)
                                .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.PIECE_COUNTING_WITHOUT_EVIDENCE));
    }

    @Test
    void activeDomainRejectsZeroActuators() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("intake")
                                .construct(MechanismConstruct.ROLLER_INTAKE)
                                .actuators(ActuatorTopology.none())
                                .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.ACTIVE_DOMAIN_WITHOUT_ACTUATOR));
    }

    @Test
    void seasonAndRobotNamesDoNotLeakIntoGenericCatalog() {
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            assertNoSeasonLeak(construct.name());
            assertNoSeasonLeak(construct.summary());
            PresetSuggestion suggestion = StandardPresets.suggestionFor(construct);
            assertNoSeasonLeak(suggestion.notes());
        }
        for (SensorRole role : SensorRole.values()) {
            assertNoSeasonLeak(role.name());
            assertNoSeasonLeak(role.typicalUse());
        }
        for (Capability capability : Capability.values()) {
            assertNoSeasonLeak(capability.name());
        }
    }

    @Test
    void configurationDoesNotWriteHardware() {
        FakeActuator actuator = new FakeActuator();
        StandardPresets.exampleConfiguration("lift", MechanismConstruct.ELEVATOR);
        MechanismConfiguration.builder("intake")
                .construct(MechanismConstruct.ROLLER_INTAKE)
                .actuators(ActuatorTopology.singleMotor())
                .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                .build();
        MechanismConfiguration.builder("claw")
                .construct(MechanismConstruct.CLAW)
                .actuators(ActuatorTopology.positionalServo())
                .enable(Capability.NAMED_STATES)
                .controlDomain(ControlDomain.NAMED_STATE)
                .namedStates("OPEN", "CLOSED", "HOLDING")
                .build();
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
    }

    @Test
    void validateReportsWithoutThrowing() {
        ValidationResult result =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.singleMotor())
                        .enable(Capability.HOMING)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .validate();
        assertFalse(result.valid());
        assertTrue(result.hasCode(ConfigurationValidator.HOMING_WITHOUT_REFERENCE));
        assertFalse(result.summary().isEmpty());
    }

    @Test
    void requiredSensorCannotIgnoreOptional() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.singleMotor())
                                .requiredSensor("home", SensorRole.RETRACT_LIMIT)
                                .enable(Capability.HOMING)
                                .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                                .degradedBehavior(SensorRole.RETRACT_LIMIT, DegradedBehavior.IGNORE_OPTIONAL)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.REQUIRED_SENSOR_IGNORE_OPTIONAL));
    }

    private static void assertNoSeasonLeak(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("biobuzz"), text);
        assertFalse(lower.contains("nectar"), text);
        assertFalse(lower.contains("decode"), text);
        assertFalse(lower.contains("bumblebee"), text);
        assertFalse(lower.contains("flower"), text);
        assertFalse(lower.contains("pollen"), text);
    }
}
