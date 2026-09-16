package org.allsparks.mimic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
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
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.observe.MechanismSnapshot;
import org.allsparks.mimic.observe.Readiness;
import org.allsparks.mimic.observe.SensorSample;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class InterlockRuleTest {

    private static final double MIN_VEL = 100.0;
    private static final double HYSTERESIS = 10.0;
    private static final long DWELL = 1_000L;

    @Test
    void sketchDeclaresNamedFeederRequiresLauncherReady() {
        InterlockRule rule =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.REJECT);
        assertEquals("feeder", rule.source());
        assertEquals("launcher", rule.requiredMechanism());
        assertEquals(InterlockRule.READY, rule.requiredState());
        assertEquals(InterlockOutcome.REJECT, rule.onFail());
        assertEquals("feeder-requires-launcher-READY", rule.name());
        assertEquals(2, rule.sources().size());
        assertEquals("feeder", rule.sources().get(0));
        assertEquals("launcher", rule.sources().get(1));
        assertTrue(rule.validate().valid());
        assertFalse(rule.permitsMotion());
        assertEquals(GoalDisposition.REJECTED, InterlockOutcome.REJECT.disposition());
    }

    @Test
    void namedIntakeMayRunOnlyWhenDeployed() {
        InterlockRule rule =
                InterlockRule.when("intake")
                        .named("intake-may-run-only-when-deployed")
                        .requires("deployer", "DEPLOYED")
                        .onFail(InterlockOutcome.REJECT);
        assertEquals("intake-may-run-only-when-deployed", rule.name());
        MechanismConfiguration deployer =
                MechanismConfiguration.builder("deployer")
                        .construct(MechanismConstruct.DEPLOYABLE_INTAKE)
                        .actuators(ActuatorTopology.positionalServo())
                        .controlDomain(ControlDomain.NAMED_STATE)
                        .enable(Capability.NAMED_STATES)
                        .namedStates("STOWED", "DEPLOYED")
                        .build();
        assertTrue(deployer.namedStates().contains(rule.requiredState()));
        InterlockInputs stowed =
                InterlockInputs.empty().withNamedState("deployer", "STOWED");
        GoalResult blocked = rule.evaluate(stowed);
        assertFalse(blocked.accepted());
        assertEquals(GoalDisposition.REJECTED, blocked.disposition());
        assertEquals(InterlockRule.REJECTED, blocked.reason());

        InterlockInputs deployed =
                InterlockInputs.empty().withNamedState("deployer", "DEPLOYED");
        GoalResult allowed = rule.evaluate(deployed);
        assertTrue(allowed.accepted());
        assertEquals(GoalDisposition.ACCEPTED, allowed.disposition());
        assertEquals(InterlockRule.SATISFIED, allowed.reason());
        assertFalse(rule.permitsMotion());
    }

    @Test
    void rejectDeferClampTables() {
        Object[][] rows = {
            {
                InterlockOutcome.REJECT,
                GoalDisposition.REJECTED,
                InterlockRule.REJECTED,
                null,
                null
            },
            {
                InterlockOutcome.DEFER,
                GoalDisposition.DEFERRED,
                InterlockRule.DEFERRED,
                null,
                null
            },
            {
                InterlockOutcome.CLAMP,
                GoalDisposition.CLAMPED,
                InterlockRule.CLAMPED,
                "STOPPED",
                null
            }
        };
        InterlockInputs missing = InterlockInputs.empty();
        InterlockInputs ready =
                InterlockInputs.empty().withReady("launcher", true);
        for (Object[] row : rows) {
            InterlockOutcome outcome = (InterlockOutcome) row[0];
            GoalDisposition disposition = (GoalDisposition) row[1];
            String reason = (String) row[2];
            String clampTo = (String) row[3];
            InterlockRule.Builder builder =
                    InterlockRule.when("feeder").requires("launcher", InterlockRule.READY);
            if (clampTo != null) {
                builder.clampTo(clampTo);
            }
            InterlockRule rule = builder.onFail(outcome);
            assertTrue(rule.validate().valid(), outcome.name());
            assertEquals(disposition, outcome.disposition(), outcome.name());

            GoalResult failed = rule.evaluate(missing);
            assertFalse(failed.accepted(), outcome.name());
            assertEquals(disposition, failed.disposition(), outcome.name());
            assertEquals(reason, failed.reason(), outcome.name());
            if (clampTo != null) {
                assertEquals(clampTo, rule.clampTo());
            }

            GoalResult passed = rule.evaluate(ready);
            assertTrue(passed.accepted(), outcome.name());
            assertEquals(GoalDisposition.ACCEPTED, passed.disposition(), outcome.name());
            assertEquals(InterlockRule.SATISFIED, passed.reason(), outcome.name());
            assertFalse(rule.permitsMotion(), outcome.name());
        }
    }

    @Test
    void confirmMapsToDeferredWithoutAddingDisposition() {
        InterlockRule rule =
                InterlockRule.when("intake")
                        .named("intake-confirm-deploy")
                        .requires("deployer", "DEPLOYED")
                        .onFail(InterlockOutcome.CONFIRM);
        GoalResult result = rule.evaluate(InterlockInputs.empty());
        assertFalse(result.accepted());
        assertEquals(GoalDisposition.DEFERRED, result.disposition());
        assertEquals(InterlockRule.CONFIRM_REQUIRED, result.reason());
        assertEquals(GoalDisposition.DEFERRED, InterlockOutcome.CONFIRM.disposition());
        for (GoalDisposition value : GoalDisposition.values()) {
            assertNotEquals("CONFIRM", value.name());
        }
        assertFalse(rule.permitsMotion());
    }

    @Test
    void intermediateMapsToReplacedAndMustNotCycle() {
        InterlockRule spinUp =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .intermediate("launcher")
                        .onFail(InterlockOutcome.INTERMEDIATE);
        assertTrue(spinUp.validate().valid());
        GoalResult replaced = spinUp.evaluate(InterlockInputs.empty());
        assertFalse(replaced.accepted());
        assertEquals(GoalDisposition.REPLACED, replaced.disposition());
        assertEquals(InterlockRule.INTERMEDIATE_GOAL, replaced.reason());
        assertEquals("launcher", spinUp.intermediate());
        assertEquals(GoalDisposition.REPLACED, InterlockOutcome.INTERMEDIATE.disposition());

        InterlockRule feedBack =
                InterlockRule.when("launcher")
                        .requires("feeder", "FEEDING")
                        .intermediate("feeder")
                        .onFail(InterlockOutcome.INTERMEDIATE);
        assertTrue(InterlockRule.generatedIntermediatesCycle(spinUp, feedBack));
        assertFalse(InterlockRule.generatedIntermediatesCycle(spinUp, spinUpReject()));

        InterlockRule self =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .intermediate("feeder")
                        .onFail(InterlockOutcome.INTERMEDIATE);
        assertFalse(self.validate().valid());
        assertTrue(self.validate().hasCode(ConfigurationValidator.INTERLOCK_INTERMEDIATE_CYCLE));
        assertTrue(InterlockRule.generatedIntermediatesCycle(self, feedBack));
    }

    @Test
    void readinessBitSatisfiesReadyRequirementWithoutFiring() {
        FakeActuator actuator = new FakeActuator();
        Readiness notReady = Readiness.atSpeed(velocity(0L, 120.0), MIN_VEL, HYSTERESIS, DWELL);
        assertFalse(notReady.ready());
        InterlockRule rule =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.DEFER);
        GoalResult waiting =
                rule.evaluate(InterlockInputs.empty().withReadiness("launcher", notReady));
        assertEquals(GoalDisposition.DEFERRED, waiting.disposition());
        assertEquals(InterlockRule.DEFERRED, waiting.reason());

        Readiness ready = notReady.feed(velocity(DWELL, 120.0));
        assertTrue(ready.ready());
        GoalResult allowed =
                rule.evaluate(InterlockInputs.empty().withReadiness("launcher", ready));
        assertTrue(allowed.accepted());
        assertEquals(GoalDisposition.ACCEPTED, allowed.disposition());
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertFalse(rule.permitsMotion());
    }

    @Test
    void namedStateReadyAlsoSatisfiesReadyToken() {
        InterlockRule rule =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.REJECT);
        GoalResult allowed =
                rule.evaluate(InterlockInputs.empty().withNamedState("launcher", InterlockRule.READY));
        assertTrue(allowed.accepted());
        GoalResult blocked =
                rule.evaluate(InterlockInputs.empty().withNamedState("launcher", "IDLE"));
        assertEquals(GoalDisposition.REJECTED, blocked.disposition());
    }

    @Test
    void missingEvidenceFailsClosed() {
        InterlockRule rule =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.REJECT);
        assertEquals(GoalDisposition.REJECTED, rule.evaluate(InterlockInputs.empty()).disposition());
        assertEquals(
                GoalDisposition.REJECTED,
                rule.evaluate(InterlockInputs.empty().withReady("launcher", false)).disposition());
        assertThrows(NullPointerException.class, () -> rule.evaluate(null));
    }

    @Test
    void emptySourceAndRequirementAreRejected() {
        ValidationResult emptySource =
                InterlockRule.when("  ").requires("launcher", InterlockRule.READY).validate();
        assertFalse(emptySource.valid());
        assertTrue(emptySource.hasCode(ConfigurationValidator.INTERLOCK_EMPTY_SOURCE));

        ValidationResult emptyRequirement =
                InterlockRule.when("feeder").requires(" ", "").onFail(InterlockOutcome.REJECT).validate();
        assertFalse(emptyRequirement.valid());
        assertTrue(emptyRequirement.hasCode(ConfigurationValidator.INTERLOCK_EMPTY_REQUIREMENT));

        ValidationResult missingClamp =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.CLAMP)
                        .validate();
        assertFalse(missingClamp.valid());
        assertTrue(missingClamp.hasCode(ConfigurationValidator.INTERLOCK_CLAMP_TARGET_REQUIRED));

        ValidationResult missingIntermediate =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.INTERMEDIATE)
                        .validate();
        assertFalse(missingIntermediate.valid());
        assertTrue(
                missingIntermediate.hasCode(
                        ConfigurationValidator.INTERLOCK_INTERMEDIATE_TARGET_REQUIRED));
    }

    @Test
    void inputsAreImmutableAndRejectEmptyKeys() {
        InterlockInputs first = InterlockInputs.empty().withNamedState("deployer", "STOWED");
        InterlockInputs second = first.withNamedState("deployer", "DEPLOYED").withReady("launcher", true);
        assertEquals("STOWED", first.namedState("deployer").orElse(null));
        assertFalse(first.ready("launcher").isPresent());
        assertEquals("DEPLOYED", second.namedState("deployer").orElse(null));
        assertEquals(Boolean.TRUE, second.ready("launcher").orElse(null));
        assertThrows(UnsupportedOperationException.class, () -> first.namedStates().put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> first.readyBits().put("x", true));
        assertThrows(IllegalArgumentException.class, () -> InterlockInputs.empty().withNamedState(" ", "OPEN"));
        assertThrows(IllegalArgumentException.class, () -> InterlockInputs.empty().withReady("", true));
        assertThrows(
                NullPointerException.class, () -> InterlockInputs.empty().withReadiness("launcher", null));
    }

    @Test
    void sessionDoesNotRegisterRulesOrWriteMotors() {
        FakeActuator unused = new FakeActuator();
        InterlockRule rule =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .onFail(InterlockOutcome.REJECT);
        assertFalse(rule.permitsMotion());

        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware =
                new FakeMechanismHardware(
                        "feeder",
                        time::get,
                        MechanismUnits.rotaryRadians("feeder", 1.0, DirectionSign.POSITIVE));
        MimicSession session = MimicSession.create(hardware.observer());
        session.observe();
        session.periodic();
        GoalResult result = session.requestGoal(50.0);
        session.stop();
        assertFalse(result.accepted());
        assertEquals(GoalDisposition.REJECTED, result.disposition());
        assertEquals(MimicSession.NO_ACTIVE_CONTROL, result.reason());
        assertEquals(CalibrationState.UNCALIBRATED, session.calibrationState());
        assertFalse(sessionHolds(InterlockRule.class));
        assertFalse(sessionHolds(InterlockInputs.class));
        assertFalse(sessionHolds(InterlockOutcome.class));
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
        assertFalse(MimicFeatureFlags.defaults().isPhase7Interlocks());
        assertFalse(session.featureFlags().isPhase7Interlocks());
        assertEquals(0, hardware.actuator().outputWriteCount());
        assertEquals(0.0, hardware.actuator().power(), 1e-9);
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
        assertFalse(declaresHardwareWrite(InterlockRule.class));
        assertFalse(declaresHardwareWrite(InterlockInputs.class));
        assertFalse(declaresHardwareWrite(InterlockOutcome.class));
    }

    @Test
    void noSchedulerCommandOrSubsystemTypes() {
        assertNoForbiddenType(InterlockRule.class);
        assertNoForbiddenType(InterlockInputs.class);
        assertNoForbiddenType(InterlockOutcome.class);
        assertClassMissing("org.allsparks.mimic.config.Command");
        assertClassMissing("org.allsparks.mimic.config.Subsystem");
        assertClassMissing("org.allsparks.mimic.config.Scheduler");
        assertClassMissing("org.allsparks.mimic.config.InterlockManager");
        assertClassMissing("org.allsparks.mimic.api.Command");
        assertClassMissing("org.allsparks.mimic.api.Subsystem");
        assertClassMissing("org.allsparks.mimic.api.Scheduler");
    }

    @Test
    void coreTypesDoNotNameSeasonPieces() {
        assertNoSeasonLeak(InterlockRule.class.getSimpleName());
        assertNoSeasonLeak(InterlockOutcome.class.getSimpleName());
        assertNoSeasonLeak(InterlockInputs.class.getSimpleName());
        assertNoSeasonLeak(InterlockRule.class.getPackage().getName());
        assertNoSeasonLeak(InterlockRule.READY);
        assertNoSeasonLeak(InterlockRule.when("feeder").requires("launcher", InterlockRule.READY)
                .onFail(InterlockOutcome.REJECT)
                .name());
    }

    @Test
    void observerTraceDoesNotWriteActuator() {
        FakeActuator actuator = new FakeActuator();
        AtomicLong time = new AtomicLong(0L);
        MechanismObserver observer = MechanismObserver.builder(
                        "launcher",
                        time::get,
                        MechanismUnits.rotaryRadians("launcher", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .ticksPerSecond(() -> 120.0)
                .requestedOutput(actuator::power)
                .build();
        Readiness readiness = Readiness.atSpeed(observer.capture(), MIN_VEL, HYSTERESIS, DWELL);
        time.set(DWELL);
        readiness = readiness.feed(observer.capture());
        InterlockRule rule =
                InterlockRule.when("feeder")
                        .requires("launcher", InterlockRule.READY)
                        .clampTo("STOPPED")
                        .onFail(InterlockOutcome.CLAMP);
        GoalResult result =
                rule.evaluate(InterlockInputs.empty().withReadiness("launcher", readiness));
        assertTrue(readiness.ready());
        assertTrue(result.accepted());
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0, actuator.powerWriteCount());
        assertEquals(0, actuator.servoWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
    }

    private static InterlockRule spinUpReject() {
        return InterlockRule.when("feeder")
                .requires("launcher", InterlockRule.READY)
                .onFail(InterlockOutcome.REJECT);
    }

    private static MechanismSnapshot velocity(long timeNanos, double velocity) {
        SensorSample positionSample =
                new SensorSample(0.0, timeNanos, MeasurementValidity.VALID, "launcher:pos", "rad");
        SensorSample velocitySample =
                new SensorSample(velocity, timeNanos, MeasurementValidity.VALID, "launcher:vel", "rad/s");
        return new MechanismSnapshot(
                "launcher",
                positionSample,
                velocitySample,
                0.0,
                "rad",
                0.0,
                0.0,
                Double.NaN,
                LimitSwitchSample.unsupported(timeNanos, "launcher:lower"),
                LimitSwitchSample.unsupported(timeNanos, "launcher:upper"),
                SensorSample.unsupported(timeNanos, "launcher:abs", "rad"),
                SensorSample.unsupported(timeNanos, "launcher:red", "rad"),
                true,
                Double.NaN,
                timeNanos,
                0L);
    }

    private static boolean sessionHolds(Class<?> type) {
        for (Field field : MimicSession.class.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType())) {
                return true;
            }
        }
        for (Method method : MimicSession.class.getDeclaredMethods()) {
            for (Class<?> parameter : method.getParameterTypes()) {
                if (type.isAssignableFrom(parameter)) {
                    return true;
                }
            }
            if (type.isAssignableFrom(method.getReturnType())) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresHardwareWrite(Class<?> type) {
        for (Method method : type.getMethods()) {
            String name = method.getName();
            if ("setPower".equals(name) || "setPosition".equals(name) || "setVelocity".equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void assertNoForbiddenType(Class<?> type) {
        String simple = type.getSimpleName().toLowerCase(Locale.ROOT);
        assertFalse(simple.contains("command"), type.getName());
        assertFalse(simple.contains("subsystem"), type.getName());
        assertFalse(simple.contains("scheduler"), type.getName());
        assertFalse(simple.contains("manager"), type.getName());
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("schedule"), method.getName());
            assertFalse(name.contains("setpower"), method.getName());
        }
        for (Field field : type.getDeclaredFields()) {
            String fieldType = field.getType().getSimpleName();
            assertFalse("Command".equals(fieldType), field.getName());
            assertFalse("Subsystem".equals(fieldType), field.getName());
            assertFalse("Scheduler".equals(fieldType), field.getName());
        }
    }

    private static void assertClassMissing(String name) {
        try {
            Class.forName(name);
            throw new AssertionError("forbidden type present: " + name);
        } catch (ClassNotFoundException expected) {
            // Contract types must not introduce a scheduler surface.
        }
    }

    private static void assertNoSeasonLeak(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("biobuzz"), text);
        assertFalse(lower.contains("nectar"), text);
        assertFalse(lower.contains("decode"), text);
        assertFalse(lower.contains("bumblebee"), text);
        assertFalse(lower.contains("flower"), text);
        assertFalse(lower.contains("pollen"), text);
        assertFalse(lower.contains("artifact"), text);
    }
}
