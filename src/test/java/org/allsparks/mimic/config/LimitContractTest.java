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
import org.allsparks.mimic.observe.LimitSwitchSample;
import org.allsparks.mimic.observe.MeasurementValidity;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class LimitContractTest {

    @Test
    void softBuilderSketchDeclaresBoundsWithoutMoving() {
        LimitContract contract =
                LimitContract.soft(0.0, 400.0)
                        .stoppingMargin(15.0)
                        .wrapPolicy(WrapPolicy.NONE)
                        .missingSwitchRule(MissingSwitchRule.NEVER_TREAT_AS_CLEAR)
                        .build();
        assertEquals(LimitPolicy.SOFT_ONLY, contract.policy());
        assertEquals(0.0, contract.min(), 1e-9);
        assertEquals(400.0, contract.max(), 1e-9);
        assertEquals(15.0, contract.stoppingMargin(), 1e-9);
        assertEquals(WrapPolicy.NONE, contract.wrapPolicy());
        assertEquals(MissingSwitchRule.NEVER_TREAT_AS_CLEAR, contract.missingSwitchRule());
        assertTrue(contract.hasSoftBounds());
        assertFalse(contract.hasHardLimits());
        assertTrue(contract.validate().valid());
        assertFalse(contract.permitsMotion());
    }

    @Test
    void missingSampleCannotAuthorizeTravelIntoTheLimit() {
        LimitContract hard = LimitContract.hard().build();
        LimitContract both = LimitContract.softAndHard(0.0, 100.0).stoppingMargin(5.0).build();
        double interior = 50.0;

        assertFalse(hard.authorizesTravelInto(LimitSide.MAX, interior, missing("upper")));
        assertFalse(hard.authorizesTravelInto(LimitSide.MIN, interior, missing("lower")));
        assertFalse(both.authorizesTravelInto(LimitSide.MAX, interior, missing("upper")));
        assertFalse(both.authorizesTravelInto(LimitSide.MIN, interior, missing("lower")));
        assertFalse(both.authorizesTravelInto(LimitSide.MAX, interior, null));

        LimitContract softOnly = LimitContract.soft(0.0, 100.0).stoppingMargin(5.0).build();
        assertFalse(softOnly.authorizesTravelInto(LimitSide.MAX, interior, missing("upper")));
        assertTrue(softOnly.authorizesTravelInto(LimitSide.MAX, interior, unsupported("upper")));
    }

    @Test
    void switchSamplesCoverValidAssertedClearMissingAndUnsupported() {
        LimitContract contract = LimitContract.softAndHard(0.0, 100.0).stoppingMargin(5.0).build();
        double interior = 50.0;

        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, interior, asserted("upper")));
        assertFalse(contract.authorizesTravelInto(LimitSide.MIN, interior, asserted("lower")));
        assertTrue(contract.authorizesTravelInto(LimitSide.MAX, interior, clear("upper")));
        assertTrue(contract.authorizesTravelInto(LimitSide.MIN, interior, clear("lower")));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, interior, missing("upper")));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, interior, unsupported("upper")));
        assertFalse(contract.authorizesTravelInto(LimitSide.MIN, interior, missing("lower")));
        assertFalse(contract.authorizesTravelInto(LimitSide.MIN, interior, unsupported("lower")));

        assertTrue(asserted("upper").asserted());
        assertTrue(asserted("upper").isUsable());
        assertFalse(clear("upper").asserted());
        assertTrue(clear("upper").isUsable());
        assertTrue(missing("upper").missing());
        assertFalse(missing("upper").asserted());
        assertEquals(MeasurementValidity.MISSING, missing("upper").validity());
        assertTrue(unsupported("upper").unsupported());
        assertFalse(unsupported("upper").asserted());
        assertEquals(MeasurementValidity.UNSUPPORTED, unsupported("upper").validity());
    }

    @Test
    void staleAndDisagreeingSamplesDoNotAuthorizeTravelIntoTheLimit() {
        LimitContract contract = LimitContract.hard().build();
        LimitSwitchSample stale =
                new LimitSwitchSample(false, false, 0L, MeasurementValidity.STALE, "upper");
        LimitSwitchSample disagreeing =
                new LimitSwitchSample(false, false, 0L, MeasurementValidity.DISAGREEING, "upper");
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, 0.0, stale));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, 0.0, disagreeing));
    }

    @Test
    void stoppingMarginBlocksTravelIntoSoftBounds() {
        LimitContract contract = LimitContract.soft(0.0, 100.0).stoppingMargin(10.0).build();
        LimitSwitchSample noSwitch = unsupported("none");

        assertTrue(contract.authorizesTravelInto(LimitSide.MIN, 50.0, noSwitch));
        assertTrue(contract.authorizesTravelInto(LimitSide.MAX, 50.0, noSwitch));
        assertFalse(contract.authorizesTravelInto(LimitSide.MIN, 10.0, noSwitch));
        assertFalse(contract.authorizesTravelInto(LimitSide.MIN, 0.0, noSwitch));
        assertFalse(contract.authorizesTravelInto(LimitSide.MIN, -1.0, noSwitch));
        assertTrue(contract.authorizesTravelInto(LimitSide.MAX, 10.0, noSwitch));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, 90.0, noSwitch));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, 100.0, noSwitch));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, 101.0, noSwitch));
        assertTrue(contract.authorizesTravelInto(LimitSide.MIN, 90.0, noSwitch));
    }

    @Test
    void wrapAwareRotaryBoundsUseAnAllowedArc() {
        // Cable wrap at 0: allowed forward arc 10 -> 350, forbidden 350 -> 10 through 0.
        LimitContract turret =
                LimitContract.soft(10.0, 350.0).stoppingMargin(5.0).wrapAware(360.0).build();
        assertEquals(WrapPolicy.WRAP_AWARE, turret.wrapPolicy());
        assertEquals(360.0, turret.wrapPeriod(), 1e-9);
        assertTrue(turret.validate().valid());
        LimitSwitchSample noSwitch = unsupported("none");

        assertTrue(LimitContract.inAllowedArc(180.0, 10.0, 350.0, 360.0));
        assertFalse(LimitContract.inAllowedArc(0.0, 10.0, 350.0, 360.0));
        assertTrue(turret.authorizesTravelInto(LimitSide.MIN, 180.0, noSwitch));
        assertTrue(turret.authorizesTravelInto(LimitSide.MAX, 180.0, noSwitch));
        assertFalse(turret.authorizesTravelInto(LimitSide.MIN, 12.0, noSwitch));
        assertFalse(turret.authorizesTravelInto(LimitSide.MAX, 348.0, noSwitch));
        assertFalse(turret.authorizesTravelInto(LimitSide.MAX, 0.0, noSwitch));
        assertFalse(turret.authorizesTravelInto(LimitSide.MIN, 0.0, noSwitch));

        // min > max: allowed arc crosses 0 (350 -> 10).
        LimitContract shortArc =
                LimitContract.soft(350.0, 10.0).stoppingMargin(1.0).wrapAware(360.0).build();
        assertTrue(shortArc.validate().valid());
        assertTrue(LimitContract.inAllowedArc(0.0, 350.0, 10.0, 360.0));
        assertFalse(LimitContract.inAllowedArc(180.0, 350.0, 10.0, 360.0));
        assertTrue(shortArc.authorizesTravelInto(LimitSide.MAX, 0.0, noSwitch));
        assertTrue(shortArc.authorizesTravelInto(LimitSide.MIN, 0.0, noSwitch));
        assertFalse(shortArc.authorizesTravelInto(LimitSide.MAX, 180.0, noSwitch));
    }

    @Test
    void wrapAwareRequiresAPositivePeriod() {
        ValidationResult missingPeriod = LimitContract.soft(10.0, 350.0).wrapPolicy(WrapPolicy.WRAP_AWARE).validate();
        assertFalse(missingPeriod.valid());
        assertTrue(missingPeriod.hasCode(ConfigurationValidator.LIMIT_WRAP_PERIOD_REQUIRED));

        ValidationResult zeroPeriod = LimitContract.soft(10.0, 350.0).wrapAware(0.0).validate();
        assertFalse(zeroPeriod.valid());
        assertTrue(zeroPeriod.hasCode(ConfigurationValidator.LIMIT_WRAP_PERIOD_REQUIRED));
    }

    @Test
    void contractValidatesSoftBoundsAndStoppingMargin() {
        ValidationResult none = LimitContract.of(LimitPolicy.NONE).validate();
        assertFalse(none.valid());
        assertTrue(none.hasCode(ConfigurationValidator.LIMIT_POLICY_REQUIRED));

        ValidationResult missingBounds = LimitContract.of(LimitPolicy.SOFT_ONLY).validate();
        assertFalse(missingBounds.valid());
        assertTrue(missingBounds.hasCode(ConfigurationValidator.LIMIT_SOFT_BOUNDS_REQUIRED));

        ValidationResult reversed = LimitContract.soft(10.0, 0.0).validate();
        assertFalse(reversed.valid());
        assertTrue(reversed.hasCode(ConfigurationValidator.LIMIT_LINEAR_BOUNDS_ORDER));

        ValidationResult negativeMargin = LimitContract.soft(0.0, 10.0).stoppingMargin(-1.0).validate();
        assertFalse(negativeMargin.valid());
        assertTrue(negativeMargin.hasCode(ConfigurationValidator.LIMIT_INVALID_STOPPING_MARGIN));

        assertTrue(LimitContract.hard().validate().valid());
    }

    @Test
    void configurationRejectsIncompleteSoftContract() {
        InvalidMechanismConfigurationException thrown =
                assertThrows(
                        InvalidMechanismConfigurationException.class,
                        () -> liftBuilder()
                                .limitContract(LimitContract.of(LimitPolicy.SOFT_ONLY).build())
                                .build());
        assertTrue(thrown.result().hasCode(ConfigurationValidator.LIMIT_SOFT_BOUNDS_REQUIRED));
    }

    @Test
    void completeContractAttachesAndLeavesExistingBuildersValid() {
        LimitContract contract = LimitContract.softAndHard(0.0, 400.0).stoppingMargin(12.0).build();
        MechanismConfiguration withContract = liftBuilder().limitContract(contract).build();
        assertTrue(withContract.limitContract().isPresent());
        assertEquals(contract, withContract.limitContract().get());
        assertEquals(LimitPolicy.SOFT_AND_HARD, withContract.limitPolicy());
        assertTrue(withContract.validate().valid());
        assertFalse(withContract.limitContract().get().permitsMotion());

        MechanismConfiguration policyOnly = liftBuilder().limitPolicy(LimitPolicy.SOFT_AND_HARD).build();
        assertFalse(policyOnly.limitContract().isPresent());
        assertEquals(LimitPolicy.SOFT_AND_HARD, policyOnly.limitPolicy());
        assertTrue(policyOnly.validate().valid());
    }

    @Test
    void contractPolicyMustMatchConfigurationPolicy() {
        LimitContract contract = LimitContract.soft(0.0, 400.0).build();
        ValidationResult mismatch =
                liftBuilder()
                        .limitContract(contract)
                        .limitPolicy(LimitPolicy.HARD_ONLY)
                        .validate();
        assertFalse(mismatch.valid());
        assertTrue(mismatch.hasCode(ConfigurationValidator.LIMIT_POLICY_MISMATCH));
    }

    @Test
    void sessionDoesNotCallTheContractAndDoesNotWriteActuators() {
        FakeActuator unused = new FakeActuator();
        LimitContract contract = LimitContract.softAndHard(0.0, 400.0).stoppingMargin(10.0).build();
        MechanismConfiguration configuration = liftBuilder().limitContract(contract).build();
        assertTrue(configuration.limitContract().isPresent());
        assertFalse(configuration.limitContract().get().permitsMotion());

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
        LimitContract.Builder builder = LimitContract.soft(0.0, 10.0).stoppingMargin(1.0);
        LimitContract first = builder.build();
        builder.stoppingMargin(9.0).bounds(2.0, 8.0);
        assertEquals(0.0, first.min(), 1e-9);
        assertEquals(10.0, first.max(), 1e-9);
        assertEquals(1.0, first.stoppingMargin(), 1e-9);
        assertNotEquals(first, builder.build());
    }

    @Test
    void nanPoseDoesNotAuthorizeSoftTravel() {
        LimitContract contract = LimitContract.soft(0.0, 10.0).build();
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, Double.NaN, unsupported("upper")));
        assertFalse(contract.authorizesTravelInto(LimitSide.MAX, Double.POSITIVE_INFINITY, unsupported("upper")));
    }

    private static MechanismConfiguration.Builder liftBuilder() {
        return MechanismConfiguration.builder("lift")
                .construct(MechanismConstruct.ELEVATOR)
                .actuators(ActuatorTopology.singleMotor())
                .sensor("position", SensorRole.RELATIVE_POSITION)
                .sensor("home", SensorRole.RETRACT_LIMIT)
                .enable(Capability.SOFT_LIMITS)
                .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                .controlDomain(ControlDomain.PROFILED_POSITION);
    }

    private static LimitSwitchSample asserted(String channelId) {
        return new LimitSwitchSample(true, true, 0L, MeasurementValidity.VALID, channelId);
    }

    private static LimitSwitchSample clear(String channelId) {
        return new LimitSwitchSample(false, false, 0L, MeasurementValidity.VALID, channelId);
    }

    private static LimitSwitchSample missing(String channelId) {
        return LimitSwitchSample.missing(0L, channelId);
    }

    private static LimitSwitchSample unsupported(String channelId) {
        return LimitSwitchSample.unsupported(0L, channelId);
    }
}
