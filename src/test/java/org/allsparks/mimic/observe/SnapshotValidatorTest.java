package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class SnapshotValidatorTest {

    @Test
    void classifyPositionReportsStaleFromPositionSample() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = livenessObserver(time);
        observer.capture();
        time.addAndGet(50_000_001L);
        MechanismSnapshot snapshot = observer.capture();
        assertEquals(MeasurementValidity.STALE, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.STALE,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
        assertFalse(SnapshotValidator.hasUsablePosition(snapshot));
    }

    @Test
    void classifyPositionDistinguishesStaleFromMissing() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver staleObserver = livenessObserver(time);
        staleObserver.capture();
        time.addAndGet(50_000_001L);
        MeasurementValidity stale = SnapshotValidator.classifyPosition(
                staleObserver.capture(), -1000.0, 1000.0);

        MechanismSnapshot missing = MechanismObserver.builder(
                        "arm",
                        () -> 10L,
                        MechanismUnits.rotaryRadians("arm", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> Double.NaN)
                .ticksPerSecond(() -> 0.0)
                .build()
                .capture();

        assertEquals(MeasurementValidity.STALE, stale);
        assertEquals(MeasurementValidity.MISSING, missing.positionSample().validity());
        assertEquals(MeasurementValidity.MISSING,
                SnapshotValidator.classifyPosition(missing, -1000.0, 1000.0));
        assertNotEquals(stale, SnapshotValidator.classifyPosition(missing, -1000.0, 1000.0));
    }

    @Test
    void classifyPositionReportsUnsupportedWhenTicksOmitted() {
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticksPerSecond(() -> 0.0)
                .build()
                .capture();
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
        assertFalse(SnapshotValidator.hasUsablePosition(snapshot));
    }

    @Test
    void classifyPositionReportsMissingOnNaNTicks() {
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "arm",
                        () -> 10L,
                        MechanismUnits.rotaryRadians("arm", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> Double.NaN)
                .ticksPerSecond(() -> 0.0)
                .build()
                .capture();
        assertEquals(MeasurementValidity.MISSING, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.MISSING,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
        assertFalse(SnapshotValidator.hasUsablePosition(snapshot));
    }

    @Test
    void classifyPositionReportsDisagreeingWhenRedundantOffset() {
        AtomicReference<Double> primary = new AtomicReference<>(100.0);
        AtomicReference<Double> redundant = new AtomicReference<>(160.0);
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "elev",
                        () -> 1_000L,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(primary::get)
                .ticksPerSecond(() -> 0.0)
                .redundantTicks(redundant::get)
                .disagreementThreshold(10.0)
                .build()
                .capture();
        assertEquals(MeasurementValidity.VALID, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.DISAGREEING,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
    }

    @Test
    void classifyPositionIgnoresSubThresholdDisagreement() {
        AtomicReference<Double> primary = new AtomicReference<>(100.0);
        AtomicReference<Double> redundant = new AtomicReference<>(105.0);
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "elev",
                        () -> 1_000L,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(primary::get)
                .ticksPerSecond(() -> 0.0)
                .redundantTicks(redundant::get)
                .disagreementThreshold(10.0)
                .build()
                .capture();
        assertEquals(5.0, snapshot.disagreement(), 1e-9);
        assertTrue(snapshot.sensorValid());
        assertEquals(MeasurementValidity.VALID,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
    }

    @Test
    void classifyPositionReportsOutOfRange() {
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "elev",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 500.0)
                .ticksPerSecond(() -> 0.0)
                .build()
                .capture();
        assertEquals(MeasurementValidity.VALID, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.OUT_OF_RANGE,
                SnapshotValidator.classifyPosition(snapshot, -10.0, 10.0));
        assertTrue(SnapshotValidator.hasUsablePosition(snapshot));
    }

    @Test
    void hasUsablePositionFollowsPositionSampleNotAggregateSensorValid() {
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "elev",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .build()
                .capture();
        assertEquals(MeasurementValidity.VALID, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.velocitySample().validity());
        assertFalse(snapshot.sensorValid());
        assertTrue(SnapshotValidator.hasUsablePosition(snapshot));
        assertEquals(MeasurementValidity.VALID,
                SnapshotValidator.classifyPosition(snapshot, -1000.0, 1000.0));
    }

    private static MechanismObserver livenessObserver(AtomicLong time) {
        return MechanismObserver.builder(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .ticksPerSecond(() -> 0.0)
                .staleAfterNanos(50_000_000L)
                .build();
    }
}
