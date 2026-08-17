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
}
