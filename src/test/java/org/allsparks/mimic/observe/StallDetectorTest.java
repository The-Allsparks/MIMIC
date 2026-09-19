package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.mimic.MimicFeatureFlags;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class StallDetectorTest {

    private static final double HIGH_AMPS = 8.0;
    private static final double THRESHOLD_AMPS = 5.0;
    private static final double MOTION_EPS = 0.05;
    private static final long TIMEOUT = 1_000L;

    @Test
    void highCurrentZeroVelocityBelowTimeoutIsNotSuspected() {
        StallDetector detector = detector();
        StallSuspicion first = detector.update(stallTrace(0L, HIGH_AMPS, 0.0));
        assertFalse(first.suspected());
        assertFalse(first.unsupported());
        StallSuspicion beforeTimeout = detector.update(stallTrace(TIMEOUT - 1L, HIGH_AMPS, 0.0));
        assertFalse(beforeTimeout.suspected());
        assertFalse(beforeTimeout.unsupported());
    }

    @Test
    void highCurrentZeroVelocityAfterTimeoutIsSuspected() {
        StallDetector detector = detector();
        detector.update(stallTrace(0L, HIGH_AMPS, 0.0));
        StallSuspicion afterTimeout = detector.update(stallTrace(TIMEOUT, HIGH_AMPS, 0.0));
        assertTrue(afterTimeout.suspected());
        assertFalse(afterTimeout.unsupported());
    }

    @Test
    void missingCurrentIsUnsupportedNotStalled() {
        StallDetector detector = detector();
        StallSuspicion missing = detector.update(stallTrace(0L, Double.NaN, 0.0));
        assertTrue(missing.unsupported());
        assertFalse(missing.suspected());
        detector.update(stallTrace(TIMEOUT, Double.NaN, 0.0));
        StallSuspicion stillMissing = detector.update(stallTrace(TIMEOUT * 2L, Double.NaN, 0.0));
        assertTrue(stillMissing.unsupported());
        assertFalse(stillMissing.suspected());
    }

    @Test
    void unwiredObserverCurrentIsUnsupportedNotStalled() {
        MechanismSnapshot unsupported = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.rotaryRadians("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .ticksPerSecond(() -> 0.0)
                .build()
                .capture();
        assertTrue(Double.isNaN(unsupported.currentAmps()));
        StallSuspicion suspicion = detector().update(unsupported);
        assertTrue(suspicion.unsupported());
        assertFalse(suspicion.suspected());
    }

    @Test
    void nanCurrentFromObserverIsUnsupportedNotStalled() {
        FakeActuator actuator = new FakeActuator();
        actuator.simulateCurrentAmps(Double.NaN);
        actuator.simulateMotion(0.0, 0.0);
        MechanismSnapshot snapshot = observer(actuator, new AtomicLong(0L)).capture();
        assertTrue(Double.isNaN(snapshot.currentAmps()));
        StallSuspicion suspicion = detector().update(snapshot);
        assertTrue(suspicion.unsupported());
        assertFalse(suspicion.suspected());
        assertEquals(0, actuator.outputWriteCount());
    }

    @Test
    void velocityPresentIsNotStalled() {
        StallDetector detector = detector();
        detector.update(stallTrace(0L, HIGH_AMPS, 1.0));
        StallSuspicion moving = detector.update(stallTrace(TIMEOUT, HIGH_AMPS, 1.0));
        assertFalse(moving.suspected());
        assertFalse(moving.unsupported());
    }

    @Test
    void currentBelowThresholdIsNotStalled() {
        StallDetector detector = detector();
        detector.update(stallTrace(0L, 1.0, 0.0));
        StallSuspicion low = detector.update(stallTrace(TIMEOUT, 1.0, 0.0));
        assertFalse(low.suspected());
        assertFalse(low.unsupported());
    }

    @Test
    void motionDuringWindowRestartsTimeout() {
        StallDetector detector = detector();
        detector.update(stallTrace(0L, HIGH_AMPS, 0.0));
        detector.update(stallTrace(TIMEOUT - 1L, HIGH_AMPS, 1.0));
        StallSuspicion restarted = detector.update(stallTrace(TIMEOUT, HIGH_AMPS, 0.0));
        assertFalse(restarted.suspected());
        StallSuspicion afterRestart = detector.update(stallTrace(TIMEOUT * 2L, HIGH_AMPS, 0.0));
        assertTrue(afterRestart.suspected());
    }

    @Test
    void missingVelocityIsUnsupportedNotStalled() {
        StallSuspicion suspicion =
                detector().update(trace(0L, HIGH_AMPS, 0.0, MeasurementValidity.MISSING));
        assertTrue(suspicion.unsupported());
        assertFalse(suspicion.suspected());
    }

    @Test
    void jamSuspicionMatchesStallHeuristic() {
        JamDetector jam = JamDetector.of(THRESHOLD_AMPS, MOTION_EPS, TIMEOUT);
        assertFalse(jam.update(stallTrace(0L, HIGH_AMPS, 0.0)).suspected());
        JamSuspicion afterTimeout = jam.update(stallTrace(TIMEOUT, HIGH_AMPS, 0.0));
        assertTrue(afterTimeout.suspected());
        assertFalse(afterTimeout.unsupported());

        JamDetector missing = JamDetector.of(THRESHOLD_AMPS, MOTION_EPS, TIMEOUT);
        JamSuspicion unsupported = missing.update(stallTrace(0L, Double.NaN, 0.0));
        assertTrue(unsupported.unsupported());
        assertFalse(unsupported.suspected());

        JamDetector moving = JamDetector.of(THRESHOLD_AMPS, MOTION_EPS, TIMEOUT);
        moving.update(stallTrace(0L, HIGH_AMPS, 2.0));
        assertFalse(moving.update(stallTrace(TIMEOUT, HIGH_AMPS, 2.0)).suspected());
    }

    @Test
    void observerTraceDoesNotWriteActuator() {
        FakeActuator actuator = new FakeActuator();
        AtomicLong time = new AtomicLong(0L);
        MechanismObserver observer = observer(actuator, time);
        StallDetector detector = detector();
        JamDetector jam = JamDetector.of(THRESHOLD_AMPS, MOTION_EPS, TIMEOUT);

        actuator.simulateCurrentAmps(HIGH_AMPS);
        actuator.simulateMotion(0.0, 0.0);
        time.set(0L);
        StallSuspicion below = detector.update(observer.capture());
        assertFalse(below.suspected());
        assertFalse(jam.update(observer.capture()).suspected());

        time.set(TIMEOUT - 1L);
        assertFalse(detector.update(observer.capture()).suspected());

        time.set(TIMEOUT);
        StallSuspicion after = detector.update(observer.capture());
        assertTrue(after.suspected());
        assertTrue(jam.update(observer.capture()).suspected());

        actuator.simulateMotion(10.0, 2.0);
        time.set(TIMEOUT + 50L);
        StallSuspicion moving = detector.update(observer.capture());
        assertFalse(moving.suspected());

        actuator.simulateCurrentAmps(Double.NaN);
        actuator.simulateMotion(0.0, 0.0);
        time.set(TIMEOUT + 100L);
        StallSuspicion missing = detector.update(observer.capture());
        assertTrue(missing.unsupported());
        assertFalse(missing.suspected());

        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0, actuator.powerWriteCount());
        assertEquals(0, actuator.servoWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
        assertFalse(MimicFeatureFlags.defaults().isPhase8Faults());
        assertFalse(MimicFeatureFlags.defaults().isPhase4ProfiledControl());
        assertFalse(MimicFeatureFlags.defaults().isPhase7Interlocks());
    }

    @Test
    void sessionDoesNotHoldOrCallDetectors() {
        FakeActuator unused = new FakeActuator();
        unused.simulateCurrentAmps(HIGH_AMPS);
        unused.simulateMotion(0.0, 0.0);
        MimicSession session = MimicSession.create(observer(unused, new AtomicLong(0L)));
        session.observe();
        session.periodic();
        session.requestGoal(50.0);
        session.stop();
        assertFalse(sessionHolds(StallDetector.class));
        assertFalse(sessionHolds(JamDetector.class));
        assertFalse(sessionHolds(StallSuspicion.class));
        assertFalse(sessionHolds(JamSuspicion.class));
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
        assertFalse(session.featureFlags().isAnyActuationEnabled());
        assertFalse(session.featureFlags().isPhase8Faults());
    }

    @Test
    void timeoutIsRequiredAndDetectorsDoNotExposeHardwareWrites() {
        assertFalse(declaresHardwareWrite(StallDetector.class));
        assertFalse(declaresHardwareWrite(JamDetector.class));
        assertFalse(declaresHardwareWrite(StallSuspicion.class));
        assertFalse(declaresHardwareWrite(JamSuspicion.class));
        Method update = assertMethod(StallDetector.class, "update", MechanismSnapshot.class);
        assertEquals(StallSuspicion.class, update.getReturnType());
        Method jamUpdate = assertMethod(JamDetector.class, "update", MechanismSnapshot.class);
        assertEquals(JamSuspicion.class, jamUpdate.getReturnType());
        assertThrows(NullPointerException.class, () -> detector().update(null));
        assertThrows(IllegalArgumentException.class, () -> StallDetector.of(THRESHOLD_AMPS, MOTION_EPS, 0L));
        assertThrows(IllegalArgumentException.class, () -> StallDetector.of(THRESHOLD_AMPS, MOTION_EPS, -1L));
        assertThrows(IllegalArgumentException.class, () -> StallDetector.of(0.0, MOTION_EPS, TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> StallDetector.of(THRESHOLD_AMPS, -1.0, TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> JamDetector.of(THRESHOLD_AMPS, MOTION_EPS, 0L));
        assertThrows(NullPointerException.class, () -> JamSuspicion.from(null));
    }

    @Test
    void coreTypesDoNotNameSeasonPieces() {
        assertNoSeasonLeak(StallDetector.class.getSimpleName());
        assertNoSeasonLeak(JamDetector.class.getSimpleName());
        assertNoSeasonLeak(StallSuspicion.class.getSimpleName());
        assertNoSeasonLeak(JamSuspicion.class.getSimpleName());
        assertNoSeasonLeak(StallDetector.class.getPackage().getName());
    }

    private static StallDetector detector() {
        return StallDetector.of(THRESHOLD_AMPS, MOTION_EPS, TIMEOUT);
    }

    private static MechanismSnapshot stallTrace(long timeNanos, double currentAmps, double velocity) {
        return trace(timeNanos, currentAmps, velocity, MeasurementValidity.VALID);
    }

    private static MechanismSnapshot trace(
            long timeNanos, double currentAmps, double velocity, MeasurementValidity velocityValidity) {
        SensorSample positionSample =
                new SensorSample(0.0, timeNanos, MeasurementValidity.VALID, "intake:pos", "rad");
        SensorSample velocitySample =
                new SensorSample(velocity, timeNanos, velocityValidity, "intake:vel", "rad/s");
        return new MechanismSnapshot(
                "intake",
                positionSample,
                velocitySample,
                0.0,
                "rad",
                0.0,
                0.0,
                currentAmps,
                LimitSwitchSample.unsupported(timeNanos, "intake:lower"),
                LimitSwitchSample.unsupported(timeNanos, "intake:upper"),
                SensorSample.unsupported(timeNanos, "intake:abs", "rad"),
                SensorSample.unsupported(timeNanos, "intake:red", "rad"),
                positionSample.isUsable()
                        && (velocitySample.validity() == MeasurementValidity.UNSUPPORTED
                                || velocitySample.isUsable()),
                Double.NaN,
                timeNanos,
                0L);
    }

    private static MechanismObserver observer(FakeActuator actuator, AtomicLong time) {
        return MechanismObserver.builder(
                        "intake",
                        time::get,
                        MechanismUnits.rotaryRadians("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .currentAmps(actuator::currentAmps)
                .requestedOutput(actuator::power)
                .build();
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

    private static Method assertMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("missing " + name, ex);
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
