package org.allsparks.mimic.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.mimic.MimicFeatureFlags;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.api.CalibrationState;
import org.allsparks.mimic.api.GoalDisposition;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.api.MechanismStatus;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.fake.FakeMechanismHardware;
import org.allsparks.mimic.log.MimicEventLogger;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.observe.MechanismSnapshot;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class FaultPolicyTest {

    @Test
    void sketchMapsDisagreementPlusStopToStopMechanism() {
        assertEquals(
                FaultSeverity.STOP_MECHANISM,
                FaultPolicy.severity(
                        FaultKind.SENSOR_DISAGREEMENT, DegradedBehavior.STOP_MECHANISM));
        assertFalse(FaultPolicy.defaults().permitsMotion());
        assertFalse(FaultPolicy.defaults().permitsRecoveryMotion());
        assertFalse(FaultPolicy.defaults().changesOutput());
    }

    @Test
    void eachCatalogKindHasADefaultSeverity() {
        Set<String> catalog = new HashSet<>(
                Arrays.asList(
                        "invalid",
                        "disagree",
                        "unexpected limit",
                        "stall",
                        "jam",
                        "skew",
                        "failed home",
                        "timeout",
                        "unexpected motion",
                        "lost calibration",
                        "insufficient AMPER grant",
                        "latch unknown"));
        assertEquals(12, FaultKind.values().length);
        assertEquals(catalog.size(), FaultKind.values().length);
        for (FaultKind kind : FaultKind.values()) {
            assertTrue(catalog.remove(kind.catalogName()), kind.name());
            assertEquals(kind.defaultSeverity(), FaultPolicy.severity(kind), kind.name());
            assertFalse(FaultPolicy.autoReleases(kind), kind.name());
        }
        assertTrue(catalog.isEmpty());
        assertEquals(FaultSeverity.DEGRADED, FaultKind.SENSOR_INVALID.defaultSeverity());
        assertEquals(FaultSeverity.DEGRADED, FaultKind.SENSOR_DISAGREEMENT.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.UNEXPECTED_LIMIT.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.STALL.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.JAM.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.SKEW.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.FAILED_HOME.defaultSeverity());
        assertEquals(FaultSeverity.DEGRADED, FaultKind.TIMEOUT.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.UNEXPECTED_MOTION.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.LOST_CALIBRATION.defaultSeverity());
        assertEquals(FaultSeverity.DEGRADED, FaultKind.INSUFFICIENT_AMPER_GRANT.defaultSeverity());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultKind.LATCH_UNKNOWN.defaultSeverity());
    }

    @Test
    void optionalSensorKindsFollowDegradedDeclaration() {
        Object[][] rows = {
            {DegradedBehavior.IGNORE_OPTIONAL, FaultSeverity.INFO},
            {DegradedBehavior.MARK_DEGRADED, FaultSeverity.DEGRADED},
            {DegradedBehavior.DISABLE_CAPABILITY, FaultSeverity.DEGRADED},
            {DegradedBehavior.STOP_MECHANISM, FaultSeverity.STOP_MECHANISM}
        };
        for (FaultKind kind :
                new FaultKind[] {FaultKind.SENSOR_INVALID, FaultKind.SENSOR_DISAGREEMENT}) {
            assertTrue(kind.allowsIgnoreOptional(), kind.name());
            for (Object[] row : rows) {
                DegradedBehavior behavior = (DegradedBehavior) row[0];
                FaultSeverity expected = (FaultSeverity) row[1];
                assertEquals(
                        expected, FaultPolicy.severity(kind, behavior), kind.name() + "+" + behavior);
            }
        }
    }

    @Test
    void latchUnknownDoesNotAutoRelease() {
        for (DegradedBehavior behavior : DegradedBehavior.values()) {
            assertEquals(
                    FaultSeverity.STOP_MECHANISM,
                    FaultPolicy.severity(FaultKind.LATCH_UNKNOWN, behavior),
                    behavior.name());
            assertFalse(FaultPolicy.autoReleases(FaultKind.LATCH_UNKNOWN, behavior), behavior.name());
        }
        assertFalse(FaultKind.LATCH_UNKNOWN.allowsIgnoreOptional());
        assertFalse(FaultPolicy.autoReleases(FaultKind.LATCH_UNKNOWN));
        assertFalse(FaultPolicy.defaults().permitsMotion());
        assertFalse(FaultPolicy.defaults().permitsRecoveryMotion());
    }

    @Test
    void ignoreOptionalCannotLowerSafetyFloors() {
        FaultKind[] floors = {
            FaultKind.UNEXPECTED_LIMIT,
            FaultKind.STALL,
            FaultKind.JAM,
            FaultKind.SKEW,
            FaultKind.FAILED_HOME,
            FaultKind.UNEXPECTED_MOTION,
            FaultKind.LOST_CALIBRATION,
            FaultKind.LATCH_UNKNOWN
        };
        for (FaultKind kind : floors) {
            assertFalse(kind.allowsIgnoreOptional(), kind.name());
            assertEquals(
                    FaultSeverity.STOP_MECHANISM,
                    FaultPolicy.severity(kind, DegradedBehavior.IGNORE_OPTIONAL),
                    kind.name());
            assertEquals(
                    FaultSeverity.STOP_MECHANISM,
                    FaultPolicy.severity(kind, DegradedBehavior.MARK_DEGRADED),
                    kind.name());
        }
        assertEquals(
                FaultSeverity.DEGRADED,
                FaultPolicy.severity(FaultKind.TIMEOUT, DegradedBehavior.IGNORE_OPTIONAL));
        assertEquals(
                FaultSeverity.STOP_MECHANISM,
                FaultPolicy.severity(FaultKind.TIMEOUT, DegradedBehavior.STOP_MECHANISM));
        assertEquals(
                FaultSeverity.DEGRADED,
                FaultPolicy.severity(
                        FaultKind.INSUFFICIENT_AMPER_GRANT, DegradedBehavior.IGNORE_OPTIONAL));
        assertEquals(
                FaultSeverity.STOP_MECHANISM,
                FaultPolicy.severity(
                        FaultKind.INSUFFICIENT_AMPER_GRANT, DegradedBehavior.STOP_MECHANISM));
        assertFalse(FaultPolicy.defaults().changesOutput());
    }

    @Test
    void documentedSeveritiesExistAndRankIncreases() {
        assertEquals(5, FaultSeverity.values().length);
        assertEquals(0, FaultSeverity.INFO.rank());
        assertEquals(1, FaultSeverity.DEGRADED.rank());
        assertEquals(2, FaultSeverity.STOP_MECHANISM.rank());
        assertEquals(3, FaultSeverity.STOP_DEPENDENCIES.rank());
        assertEquals(4, FaultSeverity.STOP_ROBOT.rank());
        assertEquals(FaultSeverity.STOP_MECHANISM, FaultSeverity.stricter(FaultSeverity.INFO, FaultSeverity.STOP_MECHANISM));
        for (FaultKind kind : FaultKind.values()) {
            assertNotEquals(FaultSeverity.STOP_ROBOT, FaultPolicy.severity(kind), kind.name());
            assertNotEquals(FaultSeverity.STOP_DEPENDENCIES, FaultPolicy.severity(kind), kind.name());
        }
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

    @Test
    void redundantDisagreementUsesDeclaredStopWithoutWriting() {
        MechanismConfiguration configuration =
                MechanismConfiguration.builder("lift")
                        .construct(MechanismConstruct.ELEVATOR)
                        .actuators(ActuatorTopology.singleMotor())
                        .sensor("primary", SensorRole.RELATIVE_POSITION)
                        .sensor("backup", SensorRole.REDUNDANT_POSITION)
                        .degradedBehavior(SensorRole.REDUNDANT_POSITION, DegradedBehavior.STOP_MECHANISM)
                        .controlDomain(ControlDomain.PROFILED_POSITION)
                        .build();
        assertEquals(
                FaultSeverity.STOP_MECHANISM,
                FaultPolicy.severity(
                        FaultKind.SENSOR_DISAGREEMENT,
                        configuration.degradedBehavior(SensorRole.REDUNDANT_POSITION)));
        FakeActuator actuator = new FakeActuator();
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
    }

    @Test
    void sessionStatusStaysDegradedOnInvalidSnapshotOnly() {
        FakeActuator unused = new FakeActuator();
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware =
                new FakeMechanismHardware(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE));
        MimicSession validSession = MimicSession.create(hardware.observer());
        assertEquals(MechanismStatus.OBSERVING, validSession.status());
        MechanismSnapshot valid = validSession.observe();
        assertTrue(valid.sensorValid());
        assertEquals(MechanismStatus.OBSERVING, validSession.status());
        assertNotEquals(MechanismStatus.FAULTED, validSession.status());
        assertNotEquals(MechanismStatus.STOPPED, validSession.status());

        MechanismObserver invalidObserver =
                MechanismObserver.builder(
                                "elev",
                                time::get,
                                MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE))
                        .ticks(() -> Double.NaN)
                        .ticksPerSecond(() -> 0.0)
                        .build();
        MimicSession invalidSession = MimicSession.create(invalidObserver);
        MechanismSnapshot invalid = invalidSession.observe();
        assertFalse(invalid.sensorValid());
        assertEquals(MechanismStatus.DEGRADED, invalidSession.status());
        assertNotEquals(MechanismStatus.FAULTED, invalidSession.status());
        assertNotEquals(MechanismStatus.STOPPED, invalidSession.status());

        GoalResult result = invalidSession.requestGoal(50.0);
        invalidSession.stop();
        assertFalse(result.accepted());
        assertEquals(GoalDisposition.REJECTED, result.disposition());
        assertEquals(MimicSession.NO_ACTIVE_CONTROL, result.reason());
        assertEquals(CalibrationState.UNCALIBRATED, invalidSession.calibrationState());
        assertFalse(sessionHolds(FaultPolicy.class));
        assertFalse(sessionHolds(FaultKind.class));
        assertFalse(sessionHolds(FaultSeverity.class));
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
        assertFalse(MimicFeatureFlags.defaults().isPhase8Faults());
        assertFalse(validSession.featureFlags().isPhase8Faults());
        assertEquals(0, hardware.actuator().outputWriteCount());
        assertEquals(0.0, hardware.actuator().power(), 1e-9);
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
        assertFalse(declaresHardwareWrite(FaultPolicy.class));
        assertFalse(declaresHardwareWrite(FaultKind.class));
        assertFalse(declaresHardwareWrite(FaultSeverity.class));
    }

    @Test
    void phase8FaultsStayOffAndSessionRejectsActuationFlag() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware =
                new FakeMechanismHardware(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE));
        MimicFeatureFlags flags = MimicFeatureFlags.builder().phase8Faults(true).build();
        assertTrue(flags.isPhase8Faults());
        assertTrue(flags.isAnyActuationEnabled());
        assertThrows(
                IllegalArgumentException.class,
                () -> new MimicSession(flags, hardware.observer(), new MimicEventLogger(8)));
        assertFalse(MimicFeatureFlags.defaults().isPhase8Faults());
        assertEquals(0, hardware.actuator().outputWriteCount());
    }

    @Test
    void nullKindOrBehaviorIsRejected() {
        assertThrows(NullPointerException.class, () -> FaultPolicy.severity(null));
        assertThrows(
                NullPointerException.class,
                () -> FaultPolicy.severity(null, DegradedBehavior.MARK_DEGRADED));
        assertThrows(
                NullPointerException.class, () -> FaultPolicy.severity(FaultKind.STALL, null));
        assertThrows(NullPointerException.class, () -> FaultPolicy.autoReleases(null));
        assertThrows(
                NullPointerException.class,
                () -> FaultPolicy.autoReleases(FaultKind.LATCH_UNKNOWN, null));
        assertThrows(NullPointerException.class, () -> FaultSeverity.fromDegradedBehavior(null));
    }

    @Test
    void coreTypesStayDesktopJava() {
        for (Class<?> type :
                new Class<?>[] {FaultKind.class, FaultSeverity.class, FaultPolicy.class}) {
            assertNoFtcType(type);
            assertNoSeasonLeak(type.getSimpleName());
            assertNoSeasonLeak(type.getPackage().getName());
        }
        assertNoSeasonLeak(FaultKind.LATCH_UNKNOWN.catalogName());
        assertNoSeasonLeak(FaultKind.INSUFFICIENT_AMPER_GRANT.catalogName());
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

    private static void assertNoFtcType(Class<?> type) {
        assertFalse(type.getName().startsWith("com.qualcomm"), type.getName());
        assertFalse(type.getName().startsWith("org.firstinspires"), type.getName());
        for (Field field : type.getDeclaredFields()) {
            String name = field.getType().getName();
            assertFalse(name.startsWith("com.qualcomm"), field.getName());
            assertFalse(name.startsWith("org.firstinspires"), field.getName());
        }
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getReturnType().getName();
            assertFalse(name.startsWith("com.qualcomm"), method.getName());
            assertFalse(name.startsWith("org.firstinspires"), method.getName());
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
