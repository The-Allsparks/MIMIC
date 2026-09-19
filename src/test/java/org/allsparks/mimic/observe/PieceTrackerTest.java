package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import org.allsparks.mimic.MimicFeatureFlags;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.config.SensorRole;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.fake.FakeLimitSwitch;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class PieceTrackerTest {

    @Test
    void emptyConfigIsUnknownOccupancyNotEmpty() {
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
        PieceTracker.Report report = PieceTracker.capacity(3).observe(snapshot);

        assertTrue(report.occupancyUnknown());
        assertTrue(report.countUnknown());
        assertTrue(Double.isNaN(report.count()));
        assertFalse(report.isKnownAbsent());
        assertFalse(report.isKnownPresent());
        assertFalse(report.present());
        assertEquals(MeasurementValidity.UNSUPPORTED, report.occupancyValidity());
        assertEquals(0.0, report.confidence(), 1e-9);
        assertTrue(report.identityUnknown(0));
        assertEquals("", report.identity(0));
        assertTrue(PieceObservation.from(snapshot).occupancyUnknown());
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
    }

    @Test
    void reconcileEntryPresentVersusCountZeroIsUnknown() {
        FakeActuator actuator = new FakeActuator();
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        MechanismSnapshot snapshot = observer(actuator)
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 0.0, "count")
                .build()
                .capture();
        PieceTracker.Report report =
                PieceTracker.capacity(3).identitySlot(0, "team-label").observe(snapshot);

        assertTrue(report.occupancyUnknown());
        assertTrue(report.countUnknown());
        assertTrue(Double.isNaN(report.count()));
        assertEquals(MeasurementValidity.DISAGREEING, report.occupancyValidity());
        assertEquals(0.0, report.confidence(), 1e-9);
        assertTrue(report.identityUnknown(0));
        assertEquals("", report.identity(0));
        assertTrue(report.observation().entry().isKnownPresent());
        assertTrue(report.observation().count().isKnown());
        assertEquals(0.0, report.observation().count().value(), 1e-9);
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
        assertEquals(0, actuator.powerWriteCount());
        assertEquals(0, actuator.servoWriteCount());
    }

    @Test
    void reconcileEntryAbsentVersusCountPositiveIsUnknown() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(false);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 2.0, "count")
                .build()
                .capture();
        PieceTracker.Report report = PieceTracker.capacity(3).reconcile(PieceObservation.from(snapshot));

        assertTrue(report.occupancyUnknown());
        assertTrue(report.countUnknown());
        assertTrue(Double.isNaN(report.count()));
        assertEquals(MeasurementValidity.DISAGREEING, report.occupancyValidity());
        assertEquals(0.0, report.confidence(), 1e-9);
        assertFalse(report.isKnownPresent());
        assertFalse(report.isKnownAbsent());
    }

    @Test
    void agreeingEntryAndCountAreKnownPresent() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 1.0, "count")
                .build()
                .capture();
        PieceTracker.Report report =
                PieceTracker.capacity(1).identitySlot(0, "team-label").observe(snapshot);

        assertFalse(report.occupancyUnknown());
        assertTrue(report.isKnownPresent());
        assertFalse(report.countUnknown());
        assertEquals(1.0, report.count(), 1e-9);
        assertEquals(MeasurementValidity.VALID, report.occupancyValidity());
        assertEquals(1.0, report.confidence(), 1e-9);
        assertFalse(report.identityUnknown(0));
        assertEquals("team-label", report.identity(0));
        assertEquals("team-label", report.identities().get(0));
        assertFalse(PieceTracker.capacity(1).permitsMotion());
    }

    @Test
    void agreeingEntryAbsentAndCountZeroIsKnownEmpty() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(false);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 0.0, "count")
                .build()
                .capture();
        PieceTracker.Report report = PieceTracker.capacity(3).observe(snapshot);

        assertFalse(report.occupancyUnknown());
        assertTrue(report.isKnownAbsent());
        assertFalse(report.countUnknown());
        assertEquals(0.0, report.count(), 1e-9);
        assertEquals(1.0, report.confidence(), 1e-9);
        assertTrue(report.identityUnknown(0));
    }

    @Test
    void countOnlyDoesNotInventWhenMissingAndDoesNotExceedCapacity() {
        MechanismSnapshot counted = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 2.0, "count")
                .build()
                .capture();
        PieceTracker.Report report = PieceTracker.capacity(3).observe(counted);
        assertFalse(report.occupancyUnknown());
        assertTrue(report.isKnownPresent());
        assertEquals(2.0, report.count(), 1e-9);
        assertEquals(0.5, report.confidence(), 1e-9);
        assertTrue(report.identityUnknown(0));
        assertTrue(report.identityUnknown(1));

        MechanismSnapshot overflow = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 4.0, "count")
                .build()
                .capture();
        PieceTracker.Report over = PieceTracker.capacity(3).observe(overflow);
        assertTrue(over.occupancyUnknown());
        assertTrue(over.countUnknown());
        assertTrue(Double.isNaN(over.count()));
        assertEquals(MeasurementValidity.DISAGREEING, over.occupancyValidity());
    }

    @Test
    void entryOnlyPresentLeavesCountUnknown() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .build()
                .capture();
        PieceTracker.Report report = PieceTracker.capacity(3).observe(snapshot);
        assertTrue(report.isKnownPresent());
        assertTrue(report.countUnknown());
        assertTrue(Double.isNaN(report.count()));
        assertEquals(0.5, report.confidence(), 1e-9);
        assertTrue(report.identityUnknown(0));
    }

    @Test
    void disconnectedSensorsStayUnknown() {
        FakeLimitSwitch entry = new FakeLimitSwitch();
        entry.setRawState(true);
        entry.disconnect();
        MechanismSnapshot snapshot = positionOnlyObserver()
                .namedLimit("entry", SensorRole.PIECE_ENTRY, entry::rawState, false)
                .build()
                .capture();
        PieceTracker.Report report = PieceTracker.capacity(3).observe(snapshot);
        assertTrue(report.occupancyUnknown());
        assertEquals(MeasurementValidity.MISSING, report.occupancyValidity());
        assertTrue(report.countUnknown());
        assertFalse(report.isKnownAbsent());
    }

    @Test
    void identityLabelsAreTeamCodeStringsAndUnknownWhenNotUnique() {
        PieceTracker tracker =
                PieceTracker.capacity(3).identitySlot(0, "team-label").identitySlot(2, "other-label");
        assertEquals(3, tracker.capacity());
        assertEquals("team-label", tracker.identityLabel(0));
        assertEquals("", tracker.identityLabel(1));
        assertEquals("other-label", tracker.identityLabel(2));

        MechanismSnapshot full = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 3.0, "count")
                .build()
                .capture();
        PieceTracker.Report fullReport = tracker.observe(full);
        assertEquals("team-label", fullReport.identity(0));
        assertEquals("", fullReport.identity(1));
        assertEquals("other-label", fullReport.identity(2));

        MechanismSnapshot partial = positionOnlyObserver()
                .namedSample("count", SensorRole.PIECE_COUNT, () -> 2.0, "count")
                .build()
                .capture();
        PieceTracker.Report partialReport = tracker.observe(partial);
        assertTrue(partialReport.isKnownPresent());
        assertEquals(2.0, partialReport.count(), 1e-9);
        assertTrue(partialReport.identityUnknown(0));
        assertTrue(partialReport.identityUnknown(2));
    }

    @Test
    void sessionDoesNotConstructOrCallTracker() {
        FakeActuator unused = new FakeActuator();
        MimicSession session = MimicSession.create(observer(unused).build());
        session.observe();
        session.periodic();
        session.requestGoal(50.0);
        session.stop();
        assertFalse(sessionHolds(PieceTracker.class));
        assertFalse(sessionHolds(PieceTracker.Report.class));
        assertFalse(session.featureFlags().isAnyActuationEnabled());
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
        assertFalse(PieceTracker.capacity(3).permitsMotion());
    }

    @Test
    void trackerDoesNotExposeHardwareWritesOrDebounce() {
        assertFalse(declaresHardwareWrite(PieceTracker.class));
        assertFalse(declaresHardwareWrite(PieceTracker.Report.class));
        assertFalse(declaresSpit(PieceTracker.class));
        assertFalse(declaresSpit(PieceTracker.Report.class));
        assertFalse(declaresDebounce(PieceTracker.class));
        assertFalse(declaresDebounce(PieceTracker.Report.class));
        Method observe = assertMethod(PieceTracker.class, "observe", MechanismSnapshot.class);
        assertEquals(PieceTracker.Report.class, observe.getReturnType());
        Method reconcile = assertMethod(PieceTracker.class, "reconcile", PieceObservation.class);
        assertEquals(PieceTracker.Report.class, reconcile.getReturnType());
        assertThrows(NullPointerException.class, () -> PieceTracker.capacity(3).observe(null));
        assertThrows(NullPointerException.class, () -> PieceTracker.capacity(3).reconcile(null));
        assertThrows(IllegalArgumentException.class, () -> PieceTracker.capacity(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> PieceTracker.capacity(3).identitySlot(3, "team-label"));
        assertThrows(IllegalArgumentException.class, () -> PieceTracker.capacity(3).identitySlot(0, " "));
        assertThrows(NullPointerException.class, () -> PieceTracker.capacity(3).identitySlot(0, null));
        assertNoFtcType(PieceTracker.class);
        assertNoFtcType(PieceTracker.Report.class);
    }

    @Test
    void coreTypesDoNotNameSeasonPieces() {
        assertNoSeasonLeak(PieceTracker.class.getSimpleName());
        assertNoSeasonLeak(PieceTracker.Report.class.getSimpleName());
        assertNoSeasonLeak(PieceTracker.class.getPackage().getName());
        assertNoSeasonLeak(PieceTracker.capacity(3).identityLabel(0));
        PieceTracker tracker = PieceTracker.capacity(2).identitySlot(0, "team-label");
        assertNoSeasonLeak(tracker.identityLabel(0));
        assertNoSeasonLeak(tracker.identityLabel(1));
    }

    private static MechanismObserver.Builder positionOnlyObserver() {
        return MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0);
    }

    private static MechanismObserver.Builder observer(FakeActuator actuator) {
        return MechanismObserver.builder(
                        "intake",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("intake", 1.0, DirectionSign.POSITIVE))
                .ticks(actuator::ticks)
                .ticksPerSecond(actuator::ticksPerSecond)
                .requestedOutput(actuator::power);
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

    private static boolean declaresSpit(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().toLowerCase(Locale.ROOT).contains("spit")) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresDebounce(Class<?> type) {
        if (type.getSimpleName().toLowerCase(Locale.ROOT).contains("debounce")) {
            return true;
        }
        for (Field field : type.getDeclaredFields()) {
            String name = field.getType().getName().toLowerCase(Locale.ROOT);
            if (name.contains("debounce")) {
                return true;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().toLowerCase(Locale.ROOT).contains("debounce")) {
                return true;
            }
            if (method.getReturnType().getName().toLowerCase(Locale.ROOT).contains("debounce")) {
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
