package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.allsparks.mimic.config.SensorRole;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.fake.FakeLimitSwitch;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class PieceObservationTest {

    @Test
    void missingSensorsAreUnknownOccupancyNotEmpty() {
        MechanismSnapshot snapshot = positionOnlyObserver().build().capture();
        PieceObservation pieces = PieceObservation.from(snapshot);

        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.entry().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.exit().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.count().validity());
        assertTrue(pieces.entry().isUnknown());
        assertTrue(pieces.exit().isUnknown());
        assertTrue(pieces.count().isUnknown());
        assertFalse(pieces.entry().isKnownAbsent());
        assertFalse(pieces.exit().isKnownAbsent());
        assertFalse(pieces.entry().isKnownPresent());
        assertTrue(pieces.occupancyUnknown());
        assertTrue(Double.isNaN(pieces.count().value()));
    }

    @Test
    void worksWithoutPieceSensors() {
        FakeActuator actuator = new FakeActuator();
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .requestedOutput(actuator::power)
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertTrue(snapshot.sensorValid());
        assertTrue(pieces.occupancyUnknown());
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.entry().validity());
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
    }

    @Test
    void wiredFalseEntryIsValidAbsentNotUnsupported() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(false);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.VALID, pieces.entry().validity());
        assertFalse(pieces.entry().present());
        assertTrue(pieces.entry().isKnownAbsent());
        assertFalse(pieces.entry().isUnknown());
        assertFalse(pieces.occupancyUnknown());
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.exit().validity());
        assertTrue(pieces.exit().isUnknown());
    }

    @Test
    void wiredTrueEntryIsKnownPresent() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.VALID, pieces.entry().validity());
        assertTrue(pieces.entry().present());
        assertTrue(pieces.entry().isKnownPresent());
        assertFalse(pieces.entry().isKnownAbsent());
        assertFalse(pieces.occupancyUnknown());
        assertEquals(snapshot.role(SensorRole.PIECE_ENTRY).digital().asserted(), pieces.entry().present());
    }

    @Test
    void disconnectedDigitalChannelIsMissingUnknownOccupancy() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        entry.disconnect();
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.MISSING, pieces.entry().validity());
        assertTrue(pieces.entry().isUnknown());
        assertFalse(pieces.entry().isKnownPresent());
        assertFalse(pieces.entry().isKnownAbsent());
        assertTrue(pieces.occupancyUnknown());
        assertTrue(snapshot.sensorValid());
    }

    @Test
    void invertedFakeDigitalChannelMapsPresence() {
        FakeLimitSwitch exit = new FakeLimitSwitch();
        exit.setRawState(false);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("exit", SensorRole.PIECE_EXIT, exit::rawState, true)
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.VALID, pieces.exit().validity());
        assertTrue(pieces.exit().isKnownPresent());
        assertTrue(pieces.entry().isUnknown());
    }

    @Test
    void validCountZeroIsKnownEmptyNotUnknown() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 0.0, "count")
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.VALID, pieces.count().validity());
        assertEquals(0.0, pieces.count().value(), 1e-9);
        assertTrue(pieces.count().isKnown());
        assertFalse(pieces.count().isUnknown());
        assertFalse(pieces.occupancyUnknown());
    }

    @Test
    void validCountIsExposedWithoutSeasonNames() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 2.0, "count")
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(2.0, pieces.count().value(), 1e-9);
        assertEquals(MeasurementValidity.VALID, pieces.count().validity());
        assertEquals("count", pieces.count().unitSymbol());
        assertEquals(snapshot.role(SensorRole.PIECE_COUNT).numeric().value(), pieces.count().value(), 1e-9);
    }

    @Test
    void nanCountIsMissingUnknownOccupancy() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> Double.NaN, "count")
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.MISSING, pieces.count().validity());
        assertTrue(pieces.count().isUnknown());
        assertTrue(Double.isNaN(pieces.count().value()));
        assertTrue(pieces.occupancyUnknown());
    }

    @Test
    void nullCountSupplierStaysUnsupportedUnknown() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, null, "count")
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.count().validity());
        assertTrue(pieces.count().isUnknown());
        assertTrue(pieces.occupancyUnknown());
    }

    @Test
    void numericEntryDoesNotInventDigitalPresence() {
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedSample("entry", SensorRole.PIECE_ENTRY, () -> 1.0, "count")
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.entry().validity());
        assertTrue(pieces.entry().isUnknown());
        assertFalse(pieces.entry().isKnownPresent());
        assertTrue(pieces.occupancyUnknown());
    }

    @Test
    void digitalCountDoesNotInventNumericOccupancy() {
        FakeLimitSwitch counter = new FakeLimitSwitch();
        counter.setRawState(true);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("count", SensorRole.PIECE_COUNT, counter::rawState, false)
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(MeasurementValidity.UNSUPPORTED, pieces.count().validity());
        assertTrue(pieces.count().isUnknown());
        assertTrue(pieces.occupancyUnknown());
    }

    @Test
    void observationDoesNotWriteActuators() {
        FakeActuator actuator = new FakeActuator();
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        MechanismSnapshot snapshot = MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .requestedOutput(actuator::power)
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 1.0, "count")
                .build()
                .capture();
        PieceObservation pieces = PieceObservation.from(snapshot);
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0, actuator.powerWriteCount());
        assertEquals(0, actuator.servoWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertTrue(pieces.entry().isKnownPresent());
        assertEquals(1.0, pieces.count().value(), 1e-9);
        assertFalse(pieces.occupancyUnknown());
    }

    @Test
    void coreTypesDoNotNameSeasonPieces() {
        assertNoSeasonLeak(PieceObservation.class.getSimpleName());
        assertNoSeasonLeak(PieceObservation.Presence.class.getSimpleName());
        assertNoSeasonLeak(PieceObservation.Count.class.getSimpleName());
        assertNoSeasonLeak(SensorRole.PIECE_ENTRY.name());
        assertNoSeasonLeak(SensorRole.PIECE_EXIT.name());
        assertNoSeasonLeak(SensorRole.PIECE_COUNT.name());
        assertNoSeasonLeak(SensorRole.PIECE_ENTRY.typicalUse());
        assertNoSeasonLeak(SensorRole.PIECE_COUNT.typicalUse());
    }

    private static MechanismObserver.Builder positionOnlyObserver() {
        return MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0);
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
