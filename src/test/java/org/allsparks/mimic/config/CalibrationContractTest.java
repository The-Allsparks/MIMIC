package org.allsparks.mimic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.mimic.MimicFeatureFlags;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.api.CalibrationState;
import org.allsparks.mimic.api.GoalDisposition;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.fake.FakeMechanismHardware;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class CalibrationContractTest {

    @Test
    void homeSwitchBuilderSketchDeclaresBoundsWithoutMoving() {
        CalibrationContract contract =
                CalibrationContract.homeSwitch()
                        .maxTravel(400.0)
                        .timeout(2_000_000_000L)
                        .debounce(20_000_000L)
                        .direction(DirectionSign.NEGATIVE)
                        .encoderResetPolicy(EncoderResetPolicy.ON_COMPLETION)
                        .build();
        assertEquals(CalibrationStrategy.HOME_SWITCH, contract.strategy());
        assertEquals(400.0, contract.maxTravel(), 1e-9);
        assertEquals(2_000_000_000L, contract.timeoutNanos());
        assertEquals(20_000_000L, contract.debounceNanos());
        assertEquals(DirectionSign.NEGATIVE, contract.direction());
        assertEquals(EncoderResetPolicy.ON_COMPLETION, contract.encoderResetPolicy());
        assertTrue(contract.validate().valid());
        assertFalse(contract.permitsMotion());
        assertFalse(contract.isHardStopCurrentBlocked());
    }

    @Test
    void contractValidatesMissingTimeoutAndTravel() {
        ValidationResult missingBoth = CalibrationContract.homeSwitch().validate();
        assertFalse(missingBoth.valid());
        assertTrue(missingBoth.hasCode(ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED));
        assertTrue(missingBoth.hasCode(ConfigurationValidator.CALIBRATION_MAX_TRAVEL_REQUIRED));

        ValidationResult missingTimeout = CalibrationContract.indexPulse().maxTravel(12.0).validate();
        assertFalse(missingTimeout.valid());
        assertTrue(missingTimeout.hasCode(ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED));
        assertFalse(missingTimeout.hasCode(ConfigurationValidator.CALIBRATION_MAX_TRAVEL_REQUIRED));

        ValidationResult missingTravel =
                CalibrationContract.hardStopCurrent().timeout(1_000_000_000L).validate();
        assertFalse(missingTravel.valid());
        assertTrue(missingTravel.hasCode(ConfigurationValidator.CALIBRATION_MAX_TRAVEL_REQUIRED));
        assertFalse(missingTravel.hasCode(ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED));

        ValidationResult zeroBounds =
                CalibrationContract.homeSwitch().maxTravel(0.0).timeout(0L).validate();
        assertFalse(zeroBounds.valid());
        assertTrue(zeroBounds.hasCode(ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED));
        assertTrue(zeroBounds.hasCode(ConfigurationValidator.CALIBRATION_MAX_TRAVEL_REQUIRED));
    }

    @Test
    void configurationRejectsIncompleteHomingCapableContract() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> liftBuilder()
                                .calibrationContract(CalibrationContract.homeSwitch().build())
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED));
        assertTrue(thrown.result().hasCode(ConfigurationValidator.CALIBRATION_MAX_TRAVEL_REQUIRED));

        ValidationResult indexPulse =
                liftBuilder()
                        .calibrationContract(CalibrationContract.indexPulse().maxTravel(30.0).build())
                        .validate();
        assertFalse(indexPulse.valid());
        assertTrue(indexPulse.hasCode(ConfigurationValidator.CALIBRATION_TIMEOUT_REQUIRED));
    }

    @Test
    void completeContractAttachesAndLeavesExistingBuildersValid() {
        CalibrationContract contract =
                CalibrationContract.homeSwitch().maxTravel(250.0).timeout(1_500_000_000L).build();
        MechanismConfiguration withContract = liftBuilder().calibrationContract(contract).build();
        assertTrue(withContract.calibrationContract().isPresent());
        assertEquals(contract, withContract.calibrationContract().get());
        assertEquals(CalibrationStrategy.HOME_SWITCH, withContract.calibrationStrategy());
        assertTrue(withContract.validate().valid());

        MechanismConfiguration strategyOnly =
                liftBuilder().calibrationStrategy(CalibrationStrategy.HOME_SWITCH).build();
        assertFalse(strategyOnly.calibrationContract().isPresent());
        assertEquals(CalibrationStrategy.HOME_SWITCH, strategyOnly.calibrationStrategy());
        assertTrue(strategyOnly.validate().valid());
    }

    @Test
    void hardStopCurrentContractDoesNotEnableHomingOrMotion() {
        CalibrationContract contract =
                CalibrationContract.hardStopCurrent().maxTravel(40.0).timeout(500_000_000L).build();
        assertTrue(contract.isHardStopCurrentBlocked());
        assertFalse(contract.permitsMotion());

        MechanismConfiguration declared =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.singleMotor())
                        .calibrationContract(contract)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .build();
        assertFalse(ConfigurationValidator.hasHomingReference(declared));

        ValidationResult withHoming =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.singleMotor())
                        .enable(Capability.HOMING)
                        .calibrationContract(contract)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .validate();
        assertFalse(withHoming.valid());
        assertTrue(withHoming.hasCode(ConfigurationValidator.HOMING_WITHOUT_REFERENCE));

        MechanismConfiguration withCurrent =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.singleMotor())
                        .sensor("current", SensorRole.ACTUATOR_CURRENT)
                        .enable(Capability.HOMING)
                        .calibrationContract(contract)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .build();
        assertTrue(ConfigurationValidator.hasHomingReference(withCurrent));
        assertFalse(withCurrent.calibrationContract().get().permitsMotion());
        assertTrue(withCurrent.calibrationContract().get().isHardStopCurrentBlocked());
    }

    @Test
    void contractStrategyMustMatchConfigurationStrategy() {
        CalibrationContract contract =
                CalibrationContract.homeSwitch().maxTravel(10.0).timeout(1_000L).build();
        ValidationResult mismatch =
                liftBuilder()
                        .calibrationContract(contract)
                        .calibrationStrategy(CalibrationStrategy.INDEX_PULSE)
                        .validate();
        assertFalse(mismatch.valid());
        assertTrue(mismatch.hasCode(ConfigurationValidator.CALIBRATION_STRATEGY_MISMATCH));
    }

    @Test
    void sessionStaysUncalibratedAndRejectsGoalsWhenContractPresent() {
        FakeActuator unused = new FakeActuator();
        CalibrationContract contract =
                CalibrationContract.homeSwitch().maxTravel(400.0).timeout(2_000_000_000L).build();
        MechanismConfiguration configuration = liftBuilder().calibrationContract(contract).build();
        assertTrue(configuration.calibrationContract().isPresent());
        assertFalse(configuration.calibrationContract().get().permitsMotion());

        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware =
                new FakeMechanismHardware(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE));
        MimicSession session = MimicSession.create(hardware.observer());
        session.observe();
        GoalResult result = session.requestGoal(50.0);
        assertFalse(result.accepted());
        assertEquals(GoalDisposition.REJECTED, result.disposition());
        assertEquals(MimicSession.NO_ACTIVE_CONTROL, result.reason());
        assertEquals(CalibrationState.UNCALIBRATED, session.calibrationState());
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
        assertFalse(session.featureFlags().isAnyActuationEnabled());
        assertEquals(0, hardware.actuator().outputWriteCount());
        assertEquals(0.0, hardware.actuator().power(), 1e-9);
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
    }

    @Test
    void builderMutationDoesNotChangeBuiltContract() {
        CalibrationContract.Builder builder =
                CalibrationContract.homeSwitch().maxTravel(10.0).timeout(1_000L);
        CalibrationContract first = builder.build();
        builder.maxTravel(99.0).timeout(2_000L).debounce(5L);
        assertEquals(10.0, first.maxTravel(), 1e-9);
        assertEquals(1_000L, first.timeoutNanos());
        assertEquals(0L, first.debounceNanos());
        assertNotEquals(first, builder.build());
    }

    @Test
    void nonHomingContractDoesNotRequireTravelAndTimeout() {
        CalibrationContract knownPose = CalibrationContract.of(CalibrationStrategy.KNOWN_STARTUP_POSE).build();
        assertTrue(knownPose.validate().valid());
        assertFalse(knownPose.requiresTravelAndTimeout());
        MechanismConfiguration claw =
                MechanismConfiguration.builder("claw")
                        .construct(MechanismConstruct.CLAW)
                        .actuators(ActuatorTopology.positionalServo())
                        .enable(Capability.NAMED_STATES)
                        .calibrationContract(knownPose)
                        .controlDomain(ControlDomain.NAMED_STATE)
                        .build();
        assertEquals(CalibrationStrategy.KNOWN_STARTUP_POSE, claw.calibrationStrategy());
        assertTrue(claw.calibrationContract().isPresent());
    }

    private static MechanismConfiguration.Builder liftBuilder() {
        return MechanismConfiguration.builder("lift")
                .construct(MechanismConstruct.ELEVATOR)
                .actuators(ActuatorTopology.singleMotor())
                .sensor("position", SensorRole.RELATIVE_POSITION)
                .sensor("home", SensorRole.RETRACT_LIMIT)
                .enable(Capability.HOMING)
                .controlDomain(ControlDomain.PROFILED_POSITION);
    }
}
