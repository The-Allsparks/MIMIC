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

class SyncContractTest {

    @Test
    void maxDisagreementBuilderSketchDeclaresStopWithoutMoving() {
        SyncContract contract =
                SyncContract.maxDisagreement(12.0).action(DegradedBehavior.STOP_MECHANISM).build();
        assertEquals(12.0, contract.maxDisagreement(), 1e-9);
        assertEquals(DegradedBehavior.STOP_MECHANISM, contract.action());
        assertTrue(contract.validate().valid());
        assertTrue(contract.hasUsableMaxDisagreement());
        assertFalse(contract.permitsMotion());
        assertFalse(contract.appliesSideCorrection());
    }

    @Test
    void defaultActionIsStopMechanism() {
        SyncContract contract = SyncContract.maxDisagreement(5.0).build();
        assertEquals(DegradedBehavior.STOP_MECHANISM, contract.action());
        assertTrue(contract.validate().valid());
        assertFalse(contract.permitsMotion());
    }

    @Test
    void contractValidatesNonPositiveDisagreement() {
        ValidationResult zero = SyncContract.maxDisagreement(0.0).validate();
        assertFalse(zero.valid());
        assertTrue(zero.hasCode(ConfigurationValidator.SYNC_MAX_DISAGREEMENT_REQUIRED));

        ValidationResult negative = SyncContract.maxDisagreement(-1.0).validate();
        assertFalse(negative.valid());
        assertTrue(negative.hasCode(ConfigurationValidator.SYNC_MAX_DISAGREEMENT_REQUIRED));

        ValidationResult nan = SyncContract.maxDisagreement(Double.NaN).validate();
        assertFalse(nan.valid());
        assertTrue(nan.hasCode(ConfigurationValidator.SYNC_MAX_DISAGREEMENT_REQUIRED));
    }

    @Test
    void independentTowersCanDeclareAContract() {
        SyncContract contract =
                SyncContract.maxDisagreement(8.0).action(DegradedBehavior.STOP_MECHANISM).build();
        MechanismConfiguration withContract = independentLift().syncContract(contract).build();
        assertTrue(withContract.syncContract().isPresent());
        assertEquals(contract, withContract.syncContract().get());
        assertTrue(withContract.validate().valid());
        assertFalse(withContract.actuators().impliesIndependentSynchronization());
        assertTrue(withContract.actuators().isIndependentlySensedMotors());
        assertFalse(withContract.syncContract().get().permitsMotion());
        assertFalse(withContract.syncContract().get().appliesSideCorrection());

        MechanismConfiguration absent = independentLift().build();
        assertFalse(absent.syncContract().isPresent());
        assertFalse(absent.actuators().impliesIndependentSynchronization());
    }

    @Test
    void linkedTopologyRejectsASyncContract() {
        SyncContract contract =
                SyncContract.maxDisagreement(10.0).action(DegradedBehavior.STOP_MECHANISM).build();
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> MechanismConfiguration.builder("lift")
                                .construct(MechanismConstruct.ELEVATOR)
                                .actuators(ActuatorTopology.mechanicallyLinkedMotors(2))
                                .sensor("left", SensorRole.RELATIVE_POSITION)
                                .sensor("right", SensorRole.REDUNDANT_POSITION)
                                .syncContract(contract)
                                .controlDomain(ControlDomain.PROFILED_POSITION)
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.LINKED_MOTORS_WITH_INDEPENDENT_SYNC));

        ValidationResult reported =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.mechanicallyLinkedMotors(2))
                        .sensor("left", SensorRole.RELATIVE_POSITION)
                        .sensor("right", SensorRole.REDUNDANT_POSITION)
                        .syncContract(contract)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .validate();
        assertFalse(reported.valid());
        assertTrue(reported.hasCode(ConfigurationValidator.LINKED_MOTORS_WITH_INDEPENDENT_SYNC));
    }

    @Test
    void linkedCapabilityRejectionStillHoldsWithoutAContract() {
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
    void configurationRejectsIncompleteDisagreement() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> independentLift()
                                .syncContract(SyncContract.maxDisagreement(0.0).build())
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.SYNC_MAX_DISAGREEMENT_REQUIRED));
    }

    @Test
    void sessionDoesNotUseTheContractOrWriteMotors() {
        FakeActuator unused = new FakeActuator();
        SyncContract contract =
                SyncContract.maxDisagreement(12.0).action(DegradedBehavior.STOP_MECHANISM).build();
        MechanismConfiguration configuration = independentLift().syncContract(contract).build();
        assertTrue(configuration.syncContract().isPresent());
        assertFalse(configuration.syncContract().get().permitsMotion());
        assertFalse(configuration.syncContract().get().appliesSideCorrection());

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
        assertFalse(session.featureFlags().isPhase5Synchronization());
        assertEquals(0, hardware.actuator().outputWriteCount());
        assertEquals(0.0, hardware.actuator().power(), 1e-9);
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
    }

    @Test
    void builderMutationDoesNotChangeBuiltContract() {
        SyncContract.Builder builder =
                SyncContract.maxDisagreement(4.0).action(DegradedBehavior.STOP_MECHANISM);
        SyncContract first = builder.build();
        builder.action(DegradedBehavior.MARK_DEGRADED);
        assertEquals(DegradedBehavior.STOP_MECHANISM, first.action());
        assertEquals(4.0, first.maxDisagreement(), 1e-9);
        assertNotEquals(first, builder.build());
    }

    private static MechanismConfiguration.Builder independentLift() {
        return MechanismConfiguration.builder("lift")
                .construct(MechanismConstruct.ELEVATOR)
                .actuators(ActuatorTopology.independentlySensedMotors(2))
                .sensor("left", SensorRole.RELATIVE_POSITION)
                .sensor("right", SensorRole.REDUNDANT_POSITION)
                .controlDomain(ControlDomain.PROFILED_POSITION);
    }
}
