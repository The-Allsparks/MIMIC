package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.mimic.config.SensorRole;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class MechanismSnapshotRoleTest {

    @Test
    void missingRoleIsUnsupportedNotFalse() {
        MechanismSnapshot snapshot = positionOnlyObserver().build().capture();
        RoleSample byRole = snapshot.role(SensorRole.PIECE_ENTRY);
        assertEquals(MeasurementValidity.UNSUPPORTED, byRole.digital().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, byRole.numeric().validity());
        assertFalse(byRole.digital().asserted());
        assertFalse(byRole.hasDigital());
        assertFalse(byRole.hasNumeric());

        RoleSample byName = snapshot.sample("entry");
        assertEquals(MeasurementValidity.UNSUPPORTED, byName.digital().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, byName.numeric().validity());
        assertFalse(byName.digital().asserted());
        assertTrue(snapshot.extraSamples().isEmpty());
    }

    @Test
    void wiredFalsePieceEntryIsValidNotUnsupported() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, () -> false, false)
                .build()
                .capture();
        RoleSample entry = snapshot.sample("entry");
        assertEquals(MeasurementValidity.VALID, entry.digital().validity());
        assertFalse(entry.digital().asserted());
        assertTrue(entry.hasDigital());
        assertFalse(entry.hasNumeric());
        assertEquals(MeasurementValidity.VALID, snapshot.role(SensorRole.PIECE_ENTRY).digital().validity());
        assertFalse(snapshot.role(SensorRole.PIECE_ENTRY).digital().asserted());
    }

    @Test
    void wiredTruePieceEntryIsAsserted() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, () -> true, false)
                .build()
                .capture();
        assertTrue(snapshot.sample("entry").digital().asserted());
        assertTrue(snapshot.role(SensorRole.PIECE_ENTRY).digital().asserted());
        assertEquals(MeasurementValidity.VALID, snapshot.sample("entry").digital().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.sample("entry").numeric().validity());
    }

    @Test
    void extraNumericSampleIsAvailableByNameAndRole() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 3.0, "count")
                .build()
                .capture();
        assertEquals(3.0, snapshot.sample("count").numeric().value(), 1e-9);
        assertEquals(MeasurementValidity.VALID, snapshot.sample("count").numeric().validity());
        assertEquals(3.0, snapshot.role(SensorRole.PIECE_COUNT).numeric().value(), 1e-9);
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.sample("count").digital().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.role(SensorRole.PIECE_EXIT).numeric().validity());
    }

    @Test
    void extrasKeepExistingSnapshotFields() {
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .ticksPerSecond(() -> 5.0)
                .lowerLimit(() -> true, false)
                .upperLimit(() -> false, false)
                .namedLimit("entry", SensorRole.PIECE_ENTRY, () -> true, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 2.0, "count")
                .build()
                .capture();
        assertEquals(100.0, snapshot.position(), 1e-9);
        assertEquals(5.0, snapshot.velocity(), 1e-9);
        assertTrue(snapshot.lowerLimit().asserted());
        assertFalse(snapshot.upperLimit().asserted());
        assertTrue(snapshot.sensorValid());
        assertEquals(MeasurementValidity.VALID, snapshot.role(SensorRole.RELATIVE_POSITION).numeric().validity());
        assertEquals(100.0, snapshot.role(SensorRole.RELATIVE_POSITION).numeric().value(), 1e-9);
        assertEquals(MeasurementValidity.VALID, snapshot.role(SensorRole.VELOCITY).numeric().validity());
        assertTrue(snapshot.role(SensorRole.RETRACT_LIMIT).digital().asserted());
        assertFalse(snapshot.role(SensorRole.EXTEND_LIMIT).digital().asserted());
        assertTrue(snapshot.sample("entry").digital().asserted());
    }

    @Test
    void extrasDoNotClearSensorValidWhenOptionalChannelIsMissing() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, () -> {
                    throw new IllegalStateException("disconnected entry");
                }, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> Double.NaN, "count")
                .build()
                .capture();
        assertTrue(snapshot.sensorValid());
        assertEquals(MeasurementValidity.MISSING, snapshot.sample("entry").digital().validity());
        assertFalse(snapshot.sample("entry").digital().asserted());
        assertEquals(MeasurementValidity.MISSING, snapshot.sample("count").numeric().validity());
        assertTrue(Double.isNaN(snapshot.sample("count").numeric().value()));
    }

    @Test
    void extraNumericParticipatesInObserverLivenessStale() {
        AtomicLong time = new AtomicLong(1_000_000L);
        MechanismObserver observer = MechanismObserver.builder(
                        "intake",
                        time::get,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .ticksPerSecond(() -> 0.0)
                .namedSample("temp", SensorRole.MOTOR_TEMPERATURE, () -> 40.0, "deg")
                .staleAfterNanos(50_000_000L)
                .build();
        observer.capture();
        time.addAndGet(50_000_001L);
        MechanismSnapshot snapshot = observer.capture();
        assertEquals(MeasurementValidity.STALE, snapshot.positionSample().validity());
        assertEquals(MeasurementValidity.STALE, snapshot.sample("temp").numeric().validity());
        assertEquals(40.0, snapshot.sample("temp").numeric().value(), 1e-9);
        assertFalse(snapshot.sensorValid());
    }

    @Test
    void namedExtrasDoNotWriteActuators() {
        FakeActuator actuator = new FakeActuator();
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .requestedOutput(actuator::power)
                .namedLimit("entry", SensorRole.PIECE_ENTRY, () -> true, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 1.0, "count")
                .build()
                .capture();
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertTrue(snapshot.sample("entry").digital().asserted());
        assertEquals(1.0, snapshot.sample("count").numeric().value(), 1e-9);
    }

    @Test
    void duplicateExtraNameIsRejected() {
        MechanismObserver.Builder builder = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, () -> true, false);
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.namedSample("entry", SensorRole.PIECE_COUNT, () -> 1.0, "count"));
    }

    @Test
    void nullSupplierIsTreatedAsUnwired() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, null, false)
                .namedSample("count", SensorRole.PIECE_COUNT, null, "count")
                .build()
                .capture();
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.sample("entry").digital().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.role(SensorRole.PIECE_COUNT).numeric().validity());
        assertTrue(snapshot.extraSamples().isEmpty());
    }

    @Test
    void extraDoesNotShadowFirstClassRole() {
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .namedSample("customPose", SensorRole.RELATIVE_POSITION, () -> 7.0, "mm")
                .build()
                .capture();
        assertEquals(100.0, snapshot.role(SensorRole.RELATIVE_POSITION).numeric().value(), 1e-9);
        assertEquals(7.0, snapshot.sample("customPose").numeric().value(), 1e-9);
    }

    private static MechanismObserver.Builder positionOnlyObserver() {
        return MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0);
    }
}
