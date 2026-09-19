package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.allsparks.mimic.MimicFeatureFlags;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class ReadinessTest {

    private static final double MIN_VEL = 100.0;
    private static final double HYSTERESIS = 10.0;
    private static final long DWELL = 1_000L;

    @Test
    void velocityBelowMinIsNotAtSpeed() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 99.0), MIN_VEL, HYSTERESIS, DWELL);
        assertFalse(readiness.ready());
        assertFalse(readiness.inBand());
        readiness = readiness.feed(velocity(DWELL, 99.0));
        assertFalse(readiness.ready());
    }

    @Test
    void oneLoopAboveMinIsInBandButNotReady() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 100.0), MIN_VEL, HYSTERESIS, DWELL);
        assertTrue(readiness.inBand());
        assertFalse(readiness.ready());
    }

    @Test
    void dwellNotElapsedStaysNotReady() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 120.0), MIN_VEL, HYSTERESIS, DWELL);
        readiness = readiness.feed(velocity(DWELL - 1L, 120.0));
        assertTrue(readiness.inBand());
        assertFalse(readiness.ready());
    }

    @Test
    void dwellElapsedWithValidSamplesIsReady() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 120.0), MIN_VEL, HYSTERESIS, DWELL);
        readiness = readiness.feed(velocity(DWELL, 120.0));
        assertTrue(readiness.inBand());
        assertTrue(readiness.ready());
    }

    @Test
    void bounceBelowHysteresisRestartsDwell() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 120.0), MIN_VEL, HYSTERESIS, DWELL);
        readiness = readiness.feed(velocity(500L, 80.0));
        assertFalse(readiness.inBand());
        assertFalse(readiness.ready());
        readiness = readiness.feed(velocity(1_500L, 120.0));
        assertTrue(readiness.inBand());
        assertFalse(readiness.ready());
        readiness = readiness.feed(velocity(2_500L, 120.0));
        assertTrue(readiness.ready());
    }

    @Test
    void hysteresisHoldsReadyForASmallDip() {
        Readiness readiness = spinUpToReady();
        readiness = readiness.feed(velocity(DWELL + 100L, 95.0));
        assertTrue(readiness.inBand());
        assertTrue(readiness.ready());
    }

    @Test
    void droppingThroughHysteresisClearsReady() {
        Readiness readiness = spinUpToReady();
        readiness = readiness.feed(velocity(DWELL + 100L, 89.0));
        assertFalse(readiness.inBand());
        assertFalse(readiness.ready());
    }

    @Test
    void invalidVelocityIsNotAtSpeed() {
        assertNotAtSpeed(MeasurementValidity.MISSING, Double.NaN);
        assertNotAtSpeed(MeasurementValidity.UNSUPPORTED, Double.NaN);
        assertNotAtSpeed(MeasurementValidity.STALE, 200.0);
        assertNotAtSpeed(MeasurementValidity.OUT_OF_RANGE, 200.0);
        assertNotAtSpeed(MeasurementValidity.DISAGREEING, 200.0);
        assertNotAtSpeed(MeasurementValidity.VALID, Double.NaN);
        assertNotAtSpeed(MeasurementValidity.VALID, Double.POSITIVE_INFINITY);
    }

    @Test
    void invalidSampleDuringDwellResetsWindow() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 120.0), MIN_VEL, HYSTERESIS, DWELL);
        readiness = readiness.feed(velocity(500L, 200.0, MeasurementValidity.STALE));
        assertFalse(readiness.ready());
        assertFalse(readiness.inBand());
        readiness = readiness.feed(velocity(1_500L, 120.0));
        assertTrue(readiness.inBand());
        assertFalse(readiness.ready());
        readiness = readiness.feed(velocity(2_500L, 120.0));
        assertTrue(readiness.ready());
    }

    @Test
    void staleAfterReadyClearsAtSpeed() {
        Readiness readiness = spinUpToReady();
        readiness = readiness.feed(velocity(DWELL + 50L, 200.0, MeasurementValidity.STALE));
        assertFalse(readiness.ready());
        assertFalse(readiness.inBand());
    }

    @Test
    void inToleranceNeedsDwellAndHysteresis() {
        Readiness readiness = Readiness.inTolerance(position(0L, 10.0), 10.0, 1.0, 0.5, DWELL);
        assertTrue(readiness.inBand());
        assertFalse(readiness.ready());
        readiness = readiness.feed(position(DWELL, 10.4));
        assertTrue(readiness.ready());
        readiness = readiness.feed(position(DWELL + 10L, 11.4));
        assertTrue(readiness.ready());
        readiness = readiness.feed(position(DWELL + 20L, 11.6));
        assertFalse(readiness.ready());
        assertFalse(readiness.inBand());
    }

    @Test
    void invalidPositionIsNotInTolerance() {
        Readiness readiness =
                Readiness.inTolerance(
                        position(0L, 10.0, MeasurementValidity.MISSING), 10.0, 1.0, 0.5, DWELL);
        assertFalse(readiness.ready());
        assertFalse(readiness.inBand());
    }

    @Test
    void observerTraceDoesNotWriteActuator() {
        FakeActuator actuator = new FakeActuator();
        AtomicLong time = new AtomicLong(0L);
        AtomicReference<Double> ticksPerSecond = new AtomicReference<>(50.0);
        MechanismObserver observer = MechanismObserver.builder(
                        "flywheel",
                        time::get,
                        MechanismUnits.rotaryRadians("flywheel", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .ticksPerSecond(ticksPerSecond::get)
                .requestedOutput(actuator::power)
                .build();

        ticksPerSecond.set(50.0);
        Readiness readiness = Readiness.atSpeed(observer.capture(), MIN_VEL, HYSTERESIS, DWELL);
        assertFalse(readiness.ready());

        ticksPerSecond.set(120.0);
        time.set(0L);
        readiness = Readiness.atSpeed(observer.capture(), MIN_VEL, HYSTERESIS, DWELL);
        time.set(DWELL);
        readiness = readiness.feed(observer.capture());
        assertTrue(readiness.ready());

        ticksPerSecond.set(95.0);
        time.set(DWELL + 100L);
        readiness = readiness.feed(observer.capture());
        assertTrue(readiness.ready());

        ticksPerSecond.set(80.0);
        time.set(DWELL + 200L);
        readiness = readiness.feed(observer.capture());
        assertFalse(readiness.ready());

        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0, actuator.powerWriteCount());
        assertEquals(0, actuator.servoWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
        assertFalse(MimicFeatureFlags.defaults().isPhase4ProfiledControl());
        assertFalse(MimicFeatureFlags.defaults().isPhase7Interlocks());
    }

    @Test
    void missingAndUnsupportedObserverVelocityIsNotAtSpeed() {
        MechanismSnapshot missing = MechanismObserver.builder(
                        "flywheel",
                        () -> 0L,
                        MechanismUnits.rotaryRadians("flywheel", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .ticksPerSecond(() -> Double.NaN)
                .build()
                .capture();
        assertEquals(MeasurementValidity.MISSING, missing.velocitySample().validity());
        assertFalse(Readiness.atSpeed(missing, MIN_VEL, HYSTERESIS, DWELL).ready());

        MechanismSnapshot unsupported = MechanismObserver.builder(
                        "flywheel",
                        () -> 0L,
                        MechanismUnits.rotaryRadians("flywheel", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .build()
                .capture();
        assertEquals(MeasurementValidity.UNSUPPORTED, unsupported.velocitySample().validity());
        assertFalse(Readiness.atSpeed(unsupported, MIN_VEL, HYSTERESIS, DWELL).ready());
    }

    @Test
    void sessionDoesNotHoldOrCallReadiness() {
        FakeActuator unused = new FakeActuator();
        MimicSession session = MimicSession.create(observer(unused));
        session.observe();
        session.periodic();
        session.requestGoal(50.0);
        session.stop();
        assertFalse(sessionHoldsReadiness());
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
    }

    @Test
    void readinessDoesNotExposeHardwareWrites() {
        assertFalse(declaresHardwareWrite(Readiness.class));
        Method feed = assertMethod(Readiness.class, "feed", MechanismSnapshot.class);
        assertEquals(Readiness.class, feed.getReturnType());
        assertThrows(NullPointerException.class, () -> Readiness.atSpeed(null, MIN_VEL, HYSTERESIS, DWELL));
        assertThrows(IllegalArgumentException.class, () -> Readiness.atSpeed(-1.0, HYSTERESIS, DWELL));
        assertThrows(IllegalArgumentException.class, () -> Readiness.atSpeed(MIN_VEL, -1.0, DWELL));
        assertThrows(IllegalArgumentException.class, () -> Readiness.atSpeed(MIN_VEL, HYSTERESIS, -1L));
    }

    @Test
    void coreTypesDoNotNameSeasonPieces() {
        assertNoSeasonLeak(Readiness.class.getSimpleName());
        assertNoSeasonLeak(Readiness.class.getPackage().getName());
    }

    private static Readiness spinUpToReady() {
        Readiness readiness = Readiness.atSpeed(velocity(0L, 120.0), MIN_VEL, HYSTERESIS, DWELL);
        return readiness.feed(velocity(DWELL, 120.0));
    }

    private static void assertNotAtSpeed(MeasurementValidity validity, double velocity) {
        Readiness readiness = Readiness.atSpeed(velocity(0L, velocity, validity), MIN_VEL, HYSTERESIS, DWELL);
        assertFalse(readiness.ready(), validity + " must not be at-speed");
        assertFalse(readiness.inBand(), validity + " must not stay in-band");
        readiness = readiness.feed(velocity(DWELL, velocity, validity));
        assertFalse(readiness.ready());
    }

    private static MechanismSnapshot velocity(long timeNanos, double velocity) {
        return velocity(timeNanos, velocity, MeasurementValidity.VALID);
    }

    private static MechanismSnapshot velocity(
            long timeNanos, double velocity, MeasurementValidity validity) {
        return snapshot(timeNanos, 0.0, MeasurementValidity.VALID, velocity, validity);
    }

    private static MechanismSnapshot position(long timeNanos, double position) {
        return position(timeNanos, position, MeasurementValidity.VALID);
    }

    private static MechanismSnapshot position(
            long timeNanos, double position, MeasurementValidity validity) {
        return snapshot(timeNanos, position, validity, 0.0, MeasurementValidity.VALID);
    }

    private static MechanismSnapshot snapshot(
            long timeNanos,
            double position,
            MeasurementValidity positionValidity,
            double velocity,
            MeasurementValidity velocityValidity) {
        SensorSample positionSample =
                new SensorSample(position, timeNanos, positionValidity, "flywheel:pos", "rad");
        SensorSample velocitySample =
                new SensorSample(velocity, timeNanos, velocityValidity, "flywheel:vel", "rad/s");
        return new MechanismSnapshot(
                "flywheel",
                positionSample,
                velocitySample,
                0.0,
                "rad",
                0.0,
                0.0,
                Double.NaN,
                LimitSwitchSample.unsupported(timeNanos, "flywheel:lower"),
                LimitSwitchSample.unsupported(timeNanos, "flywheel:upper"),
                SensorSample.unsupported(timeNanos, "flywheel:abs", "rad"),
                SensorSample.unsupported(timeNanos, "flywheel:red", "rad"),
                positionSample.isUsable()
                        && (velocitySample.validity() == MeasurementValidity.UNSUPPORTED
                                || velocitySample.isUsable()),
                Double.NaN,
                timeNanos,
                0L);
    }

    private static MechanismObserver observer(FakeActuator actuator) {
        return MechanismObserver.builder(
                        "flywheel",
                        () -> 0L,
                        MechanismUnits.rotaryRadians("flywheel", 1.0, DirectionSign.POSITIVE))
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .requestedOutput(actuator::power)
                .build();
    }

    private static boolean sessionHoldsReadiness() {
        for (Field field : MimicSession.class.getDeclaredFields()) {
            if (Readiness.class.isAssignableFrom(field.getType())) {
                return true;
            }
        }
        for (Method method : MimicSession.class.getDeclaredMethods()) {
            for (Class<?> parameter : method.getParameterTypes()) {
                if (Readiness.class.isAssignableFrom(parameter)) {
                    return true;
                }
            }
            if (Readiness.class.isAssignableFrom(method.getReturnType())) {
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
