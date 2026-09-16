package org.allsparks.mimic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.junit.jupiter.api.Test;

class StandardPresetsTest {

    @Test
    void optionalRolesAreExtrasNotExampleSensors() {
        PresetSuggestion elevator = StandardPresets.suggestionFor(MechanismConstruct.ELEVATOR);
        Set<SensorRole> exampleRoles = new HashSet<>();
        for (SensorDeclaration sensor : elevator.exampleSensors()) {
            exampleRoles.add(sensor.role());
        }
        assertTrue(exampleRoles.contains(SensorRole.RELATIVE_POSITION));
        assertTrue(exampleRoles.contains(SensorRole.RETRACT_LIMIT));
        assertTrue(elevator.optionalRoles().contains(SensorRole.EXTEND_LIMIT));
        assertTrue(elevator.optionalRoles().contains(SensorRole.ABSOLUTE_POSITION));
        assertTrue(elevator.optionalRoles().contains(SensorRole.REDUNDANT_POSITION));
        assertTrue(elevator.optionalRoles().contains(SensorRole.ACTUATOR_CURRENT));
        for (SensorRole optional : elevator.optionalRoles()) {
            assertFalse(exampleRoles.contains(optional), optional.name());
        }
        assertThrows(UnsupportedOperationException.class, () -> elevator.optionalRoles().add(SensorRole.VELOCITY));
        assertFalse(elevator.hazardNotes().isEmpty());
    }

    @Test
    void exampleConfigurationDoesNotWireOptionalRoles() {
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            PresetSuggestion suggestion = StandardPresets.suggestionFor(construct);
            MechanismConfiguration example =
                    StandardPresets.exampleConfiguration("example-" + construct.name(), construct);
            assertTrue(example.validate().valid(), construct.name());
            Set<SensorRole> wired = new HashSet<>();
            for (SensorDeclaration sensor : example.sensors()) {
                wired.add(sensor.role());
            }
            for (SensorRole optional : suggestion.optionalRoles()) {
                assertFalse(wired.contains(optional), construct.name() + " wired " + optional.name());
            }
        }
    }

    @Test
    void suggestionListsDoNotAutoEnableSync() {
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            PresetSuggestion suggestion = StandardPresets.suggestionFor(construct);
            assertFalse(
                    suggestion.capabilities().contains(Capability.MULTI_ACTUATOR_SYNCHRONIZATION),
                    construct.name());
            MechanismConfiguration example =
                    StandardPresets.exampleConfiguration("example-" + construct.name(), construct);
            assertFalse(
                    example.capabilities().contains(Capability.MULTI_ACTUATOR_SYNCHRONIZATION),
                    construct.name());
            assertFalse(example.actuators().impliesIndependentSynchronization(), construct.name());
        }
        PresetSuggestion elevator = StandardPresets.suggestionFor(MechanismConstruct.ELEVATOR);
        assertTrue(elevator.optionalRoles().contains(SensorRole.REDUNDANT_POSITION));
        assertEquals(1, elevator.topology().actuatorCount());
    }

    @Test
    void optionalRolesDoNotCountAsCapabilityEvidence() {
        PresetSuggestion belt = StandardPresets.suggestionFor(MechanismConstruct.BELT);
        assertTrue(belt.optionalRoles().contains(SensorRole.PIECE_ENTRY));
        MechanismConfiguration example = StandardPresets.exampleConfiguration("belt", MechanismConstruct.BELT);
        assertTrue(example.validate().valid());
        assertFalse(example.capabilities().contains(Capability.PIECE_COUNTING));

        InvalidMechanismConfigurationException counting =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("belt")
                                .construct(MechanismConstruct.BELT)
                                .actuators(belt.topology())
                                .controlDomain(belt.controlDomain())
                                .enable(Capability.PIECE_COUNTING)
                                .build());
        assertTrue(counting.result().hasCode(ConfigurationValidator.PIECE_COUNTING_WITHOUT_EVIDENCE));

        PresetSuggestion elevator = StandardPresets.suggestionFor(MechanismConstruct.ELEVATOR);
        assertTrue(elevator.optionalRoles().contains(SensorRole.REDUNDANT_POSITION));
        InvalidMechanismConfigurationException sync =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.independentlySensedMotors(2))
                                .sensor("left", SensorRole.RELATIVE_POSITION)
                                .enable(Capability.MULTI_ACTUATOR_SYNCHRONIZATION)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(sync.result().hasCode(ConfigurationValidator.SYNC_INSUFFICIENT_FEEDBACK));
    }

    @Test
    void declaredOptionalRoleStillDoesNotEnableSync() {
        MechanismConfiguration lift =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.independentlySensedMotors(2))
                        .sensor("leftPosition", SensorRole.RELATIVE_POSITION)
                        .sensor("rightPosition", SensorRole.REDUNDANT_POSITION)
                        .sensor("home", SensorRole.RETRACT_LIMIT)
                        .enable(Capability.HOMING)
                        .enable(Capability.SOFT_LIMITS)
                        .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .build();
        assertTrue(lift.validate().valid());
        assertFalse(lift.capabilities().contains(Capability.MULTI_ACTUATOR_SYNCHRONIZATION));
        assertFalse(lift.actuators().impliesIndependentSynchronization());
    }

    @Test
    void suggestionTextDoesNotLeakSeasonNames() {
        for (MechanismConstruct construct : MechanismConstruct.values()) {
            PresetSuggestion suggestion = StandardPresets.suggestionFor(construct);
            assertNoSeasonLeak(suggestion.notes());
            assertNoSeasonLeak(suggestion.hazardNotes());
        }
    }

    @Test
    void suggestionDoesNotWriteHardware() {
        FakeActuator actuator = new FakeActuator();
        StandardPresets.suggestionFor(MechanismConstruct.ELEVATOR).optionalRoles();
        StandardPresets.exampleConfiguration("lift", MechanismConstruct.ELEVATOR);
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertEquals(0, actuator.servoWriteCount());
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
