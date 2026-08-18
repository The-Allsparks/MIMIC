package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.allsparks.mimic.clock.MimicClock;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class MechanismObserverTest {

    @Test
    void disagreementMarksSensorsInvalid() {
        AtomicLong time = new AtomicLong(1_000L);
        AtomicReference<Double> primary = new AtomicReference<>(100.0);
        AtomicReference<Double> redundant = new AtomicReference<>(160.0);
        MechanismObserver observer = MechanismObserver.builder(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(primary::get)
                .ticksPerSecond(() -> 0.0)
                .redundantTicks(redundant::get)
                .disagreementThreshold(10.0)
                .build();
        MechanismSnapshot snapshot = observer.capture();
        assertEquals(60.0, snapshot.disagreement(), 1e-9);
        assertFalse(snapshot.sensorValid());
        assertEquals(MeasurementValidity.DISAGREEING,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
    }

    @Test
    void missingPositionDegradesCleanly() {
        MimicClock clock = () -> 10L;
        MechanismObserver observer = MechanismObserver.builder(
                        "arm",
                        clock,
                        MechanismUnits.rotaryRadians("arm", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> Double.NaN)
                .ticksPerSecond(() -> 0.0)
                .build();
        MechanismSnapshot snapshot = observer.capture();
        assertFalse(snapshot.sensorValid());
        assertTrue(Double.isNaN(snapshot.position()));
        assertEquals(MeasurementValidity.MISSING, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.MISSING,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
        assertFalse(SnapshotValidator.hasUsablePosition(snapshot));
    }

    @Test
    void unsupportedCurrentIsNotInvented() {
        MechanismObserver observer = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .ticksPerSecond(() -> 0.0)
                .build();
        MechanismSnapshot snapshot = observer.capture();
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.absoluteSensor().validity());
        assertTrue(Double.isNaN(snapshot.currentAmps()));
    }

    @Test
    void staleCheckDisabledByDefaultDoesNotMarkStale() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, 0L);
        assertPrimaryChannels(observer.capture(), MeasurementValidity.VALID);
        time.addAndGet(5_000_000_000L);
        MechanismSnapshot snapshot = observer.capture();
        assertPrimaryChannels(snapshot, MeasurementValidity.VALID);
        assertTrue(snapshot.sensorValid());
    }

    @Test
    void negativeStaleAfterNanosDisablesCheck() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, -1L);
        observer.capture();
        time.addAndGet(5_000_000_000L);
        MechanismSnapshot snapshot = observer.capture();
        assertPrimaryChannels(snapshot, MeasurementValidity.VALID);
        assertTrue(snapshot.sensorValid());
    }

    @Test
    void firstCaptureIsNeverStale() {
        AtomicLong time = new AtomicLong(9_000_000_000L);
        MechanismObserver observer = livenessObserver(time, 50_000_000L);
        MechanismSnapshot snapshot = observer.capture();
        assertPrimaryChannels(snapshot, MeasurementValidity.VALID);
        assertTrue(snapshot.sensorValid());
    }

    @Test
    void gapAboveThresholdMarksNumericSamplesStale() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, 50_000_000L);
        observer.capture();
        time.addAndGet(50_000_001L);
        MechanismSnapshot snapshot = observer.capture();
        assertPrimaryChannels(snapshot, MeasurementValidity.STALE);
        assertEquals(MeasurementValidity.STALE,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
        assertEquals(12.5, snapshot.absoluteSensor().value(), 1e-9);
        assertFalse(snapshot.sensorValid());
    }

    @Test
    void gapEqualToThresholdRemainsValid() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, 50_000_000L);
        observer.capture();
        time.addAndGet(50_000_000L);
        MechanismSnapshot snapshot = observer.capture();
        assertPrimaryChannels(snapshot, MeasurementValidity.VALID);
        assertTrue(snapshot.sensorValid());
    }

    @Test
    void gapWithinThresholdRemainsValid() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, 50_000_000L);
        observer.capture();
        time.addAndGet(49_999_999L);
        MechanismSnapshot snapshot = observer.capture();
        assertPrimaryChannels(snapshot, MeasurementValidity.VALID);
        assertTrue(snapshot.sensorValid());
    }

    @Test
    void captureAfterStaleRecoversWhenLoopIsTimely() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, 50_000_000L);
        observer.capture();
        time.addAndGet(50_000_001L);
        assertPrimaryChannels(observer.capture(), MeasurementValidity.STALE);
        time.addAndGet(1_000_000L);
        MechanismSnapshot recovered = observer.capture();
        assertPrimaryChannels(recovered, MeasurementValidity.VALID);
        assertTrue(recovered.sensorValid());
    }

    @Test
    void frozenSupplierOnFastLoopRemainsValid() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time, 50_000_000L);
        MechanismSnapshot first = observer.capture();
        time.addAndGet(20_000_000L);
        MechanismSnapshot second = observer.capture();
        assertEquals(first.absoluteSensor().value(), second.absoluteSensor().value(), 1e-9);
        assertPrimaryChannels(second, MeasurementValidity.VALID);
        assertTrue(second.sensorValid());
    }

    private static void assertPrimaryChannels(MechanismSnapshot snapshot, MeasurementValidity expected) {
        assertEquals(expected, snapshot.positionSample().validity());
        assertEquals(expected, snapshot.velocitySample().validity());
        assertEquals(expected, snapshot.absoluteSensor().validity());
    }

    private static MechanismObserver livenessObserver(AtomicLong time, long staleAfterNanos) {
        MechanismObserver.Builder builder = MechanismObserver.builder(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .ticksPerSecond(() -> 0.0)
                .absoluteSensor(() -> 12.5, "mm");
        if (staleAfterNanos != 0L) {
            builder.staleAfterNanos(staleAfterNanos);
        }
        return builder.build();
    }
}
