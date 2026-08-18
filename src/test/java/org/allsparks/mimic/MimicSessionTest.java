package org.allsparks.mimic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.mimic.api.CalibrationState;
import org.allsparks.mimic.api.GoalDisposition;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.fake.FakeMechanismHardware;
import org.allsparks.mimic.log.MimicEvent;
import org.allsparks.mimic.log.MimicEventLogger;
import org.allsparks.mimic.log.MimicEventType;
import org.allsparks.mimic.observe.MeasurementValidity;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.observe.MechanismSnapshot;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class MimicSessionTest {

    @Test
    void observeDoesNotWriteActuatorOutputs() {
        AtomicLong time = new AtomicLong(1_000_000L);
        FakeMechanismHardware hardware = hardware(time);
        hardware.actuator().simulateMotion(100.0, 20.0);
        MimicSession session = MimicSession.create(hardware.observer());

        assertEquals(0, hardware.actuator().outputWriteCount());
        session.observe();
        session.periodic();
        session.stop();
        session.requestGoal(120.0);
        assertEquals(0, hardware.actuator().outputWriteCount());
        assertEquals(0.0, hardware.actuator().power(), 1e-9);
    }

    @Test
    void phase0RejectsGoalsAndStaysUncalibrated() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = hardware(time);
        MimicSession session = MimicSession.create(hardware.observer());
        session.observe();
        GoalResult result = session.requestGoal(50.0);
        assertFalse(result.accepted());
        assertEquals(GoalDisposition.REJECTED, result.disposition());
        assertEquals(MimicSession.NO_ACTIVE_CONTROL, result.reason());
        assertEquals(CalibrationState.UNCALIBRATED, session.calibrationState());
        assertTrue(session.logger().exportCsv().contains("GOAL_REJECTED"));
    }

    @Test
    void recordsPositionVelocityAndLimits() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = hardware(time);
        hardware.actuator().simulateMotion(25.0, 10.0);
        hardware.lowerLimit().setRawState(true);
        MimicSession session = MimicSession.create(hardware.observer());
        MechanismSnapshot snapshot = session.observe();
        assertEquals(2.5, snapshot.position(), 1e-9);
        assertEquals(1.0, snapshot.velocity(), 1e-9);
        assertTrue(snapshot.lowerLimit().asserted());
        assertFalse(snapshot.upperLimit().asserted());
        assertEquals("mm", snapshot.positionUnitSymbol());
        assertTrue(session.logger().exportCsv().contains("pos="));
        assertTrue(session.logger().exportCsv().contains("posValid=VALID"));
        assertTrue(session.logger().exportCsv().contains("velValid=VALID"));
        MimicEvent observation = observationEvent(session);
        assertEquals("true", observation.fields().get("lower"));
        assertEquals("VALID", observation.fields().get("lowerValid"));
        assertEquals("false", observation.fields().get("upper"));
        assertEquals("VALID", observation.fields().get("upperValid"));
        String csv = session.logger().exportCsv();
        assertTrue(csv.contains("lower=true"));
        assertTrue(csv.contains("lowerValid=VALID"));
        assertTrue(csv.contains("upper=false"));
        assertTrue(csv.contains("upperValid=VALID"));
    }

    @Test
    void missingSensorsDegradeWithoutInventingValues() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = hardware(time);
        hardware.actuator().simulateCurrentAmps(Double.NaN);
        hardware.absolute().disconnect();
        MimicSession session = MimicSession.create(hardware.observer());
        MechanismSnapshot snapshot = session.observe();
        assertTrue(Double.isNaN(snapshot.currentAmps()));
        assertEquals(MeasurementValidity.MISSING, snapshot.absoluteSensor().validity());
        assertTrue(Double.isNaN(snapshot.absoluteSensor().value()));
    }

    @Test
    void disconnectedLimitSwitchIsMissing() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = hardware(time);
        hardware.lowerLimit().disconnect();
        MimicSession session = MimicSession.create(hardware.observer());
        MechanismSnapshot snapshot = session.observe();
        assertEquals(MeasurementValidity.MISSING, snapshot.lowerLimit().validity());
        assertFalse(snapshot.lowerLimit().asserted());
        MimicEvent observation = observationEvent(session);
        assertEquals("n/a", observation.fields().get("lower"));
        assertEquals("MISSING", observation.fields().get("lowerValid"));
        String csv = session.logger().exportCsv();
        assertTrue(csv.contains("lowerValid=MISSING"));
        assertTrue(csv.contains("lower=n/a"));
        assertFalse(csv.contains("lower=false"));
    }

    @Test
    void omittedLimitSwitchExportsUnsupported() {
        AtomicLong time = new AtomicLong(0L);
        MechanismObserver observer = MechanismObserver.builder(
                        "elev",
                        time::get,
                        MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE))
                .ticks(() -> 0.0)
                .ticksPerSecond(() -> 0.0)
                .build();
        MimicSession session = MimicSession.create(observer);
        MechanismSnapshot snapshot = session.observe();
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.lowerLimit().validity());
        assertEquals(MeasurementValidity.UNSUPPORTED, snapshot.upperLimit().validity());
        MimicEvent observation = observationEvent(session);
        assertEquals("n/a", observation.fields().get("lower"));
        assertEquals("UNSUPPORTED", observation.fields().get("lowerValid"));
        assertEquals("n/a", observation.fields().get("upper"));
        assertEquals("UNSUPPORTED", observation.fields().get("upperValid"));
        String csv = session.logger().exportCsv();
        assertTrue(csv.contains("lowerValid=UNSUPPORTED"));
        assertTrue(csv.contains("upperValid=UNSUPPORTED"));
        assertTrue(csv.contains("lower=n/a"));
        assertTrue(csv.contains("upper=n/a"));
    }

    @Test
    void estimatesAccelerationFromVelocityChange() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = hardware(time);
        MimicSession session = MimicSession.create(hardware.observer());
        hardware.actuator().simulateMotion(0.0, 0.0);
        session.observe();
        hardware.actuator().simulateMotion(10.0, 10.0);
        time.addAndGet(1_000_000_000L);
        MechanismSnapshot second = session.observe();
        assertEquals(1.0, second.acceleration(), 1e-6);
    }

    @Test
    void loopOverheadIsMeasured() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = new FakeMechanismHardware(
                "elev",
                () -> time.addAndGet(500L),
                MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE));
        MimicSession session = MimicSession.create(hardware.observer());
        session.observe();
        assertTrue(session.loopOverhead().count() >= 1L);
        assertTrue(session.loopOverhead().maxNanos() > 0L);
        assertEquals(1L, session.sampleCount());
    }

    @Test
    void actuationFlagsAreRejectedInPhase0Session() {
        AtomicLong time = new AtomicLong(0L);
        FakeMechanismHardware hardware = hardware(time);
        MimicFeatureFlags flags = MimicFeatureFlags.builder().phase4ProfiledControl(true).build();
        assertTrue(flags.isAnyActuationEnabled());
        assertThrows(IllegalArgumentException.class,
                () -> new MimicSession(flags, hardware.observer(), new MimicEventLogger(8)));
    }

    @Test
    void stopLogsWithoutWritingHardware() {
        AtomicLong time = new AtomicLong(1L);
        FakeMechanismHardware hardware = hardware(time);
        MimicSession session = MimicSession.create(hardware.observer());
        session.observe();
        session.stop();
        assertEquals(0, hardware.actuator().outputWriteCount());
        boolean found = false;
        for (MimicEvent event : session.logger().snapshot()) {
            if (event.type() == MimicEventType.STOP_REQUESTED) {
                found = true;
            }
        }
        assertTrue(found);
    }

    private static FakeMechanismHardware hardware(AtomicLong time) {
        return new FakeMechanismHardware(
                "elev",
                time::get,
                MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE));
    }

    private static MimicEvent observationEvent(MimicSession session) {
        for (MimicEvent event : session.logger().snapshot()) {
            if ("observation".equals(event.message())) {
                return event;
            }
        }
        throw new AssertionError("missing observation event");
    }
}
