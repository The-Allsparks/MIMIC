package org.allsparks.mimic.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.allsparks.mimic.MimicFeatureFlags;
import org.allsparks.mimic.MimicSession;
import org.allsparks.mimic.fake.FakeActuator;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.observe.MechanismSnapshot;
import org.allsparks.mimic.units.DirectionSign;
import org.allsparks.mimic.units.MechanismUnits;
import org.junit.jupiter.api.Test;

class MechanismControllerAdapterTest {

    @Test
    void fakeAdapterReturnsEffortWithoutWritingActuator() {
        FakeActuator unused = new FakeActuator();
        MechanismSnapshot snap = observer().capture();
        Setpoint setpoint = Setpoint.at(120.0, "mm");
        MechanismControllerAdapter adapter = new FakeMechanismControllerAdapter(0.25);

        double effort = adapter.effort(snap, setpoint);

        assertEquals(0.25, effort, 1e-9);
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
        assertTrue(Double.isNaN(unused.servoPosition()));
        assertFalse(MimicFeatureFlags.defaults().isPhase4ProfiledControl());
        assertFalse(MimicFeatureFlags.defaults().isAnyActuationEnabled());
    }

    @Test
    void sessionDoesNotHoldOrCallTheAdapter() {
        FakeActuator unused = new FakeActuator();
        MechanismControllerAdapter adapter = new FakeMechanismControllerAdapter(0.9);
        MimicSession session = MimicSession.create(observer());
        session.observe();
        session.periodic();
        session.requestGoal(50.0);
        session.stop();

        assertFalse(sessionHoldsAdapter());
        assertEquals(0, unused.outputWriteCount());
        assertEquals(0.0, unused.power(), 1e-9);
        // The fake is never handed to the session; calling it here only
        // proves the seam still compiles after a Phase 0 loop.
        assertEquals(0.9, adapter.effort(session.snapshot(), Setpoint.at(50.0, "mm")), 1e-9);
        assertEquals(0, unused.outputWriteCount());
    }

    @Test
    void adapterTypesDoNotExposeSetPower() {
        assertFalse(declaresHardwareWrite(MechanismControllerAdapter.class));
        assertFalse(declaresHardwareWrite(Setpoint.class));
        Method effort =
                assertMethod(
                        MechanismControllerAdapter.class,
                        "effort",
                        MechanismSnapshot.class,
                        Setpoint.class);
        assertEquals(double.class, effort.getReturnType());
    }

    @Test
    void gradleDoesNotDependOnNextControlOrWpiPidLibraries() throws Exception {
        String gradle = new String(Files.readAllBytes(buildGradle()), StandardCharsets.UTF_8);
        String lower = gradle.toLowerCase();
        assertFalse(lower.contains("nextcontrol"));
        assertFalse(lower.contains("nextftc"));
        assertFalse(lower.contains("ftclib"));
        assertFalse(lower.contains("wpilib"));
        assertFalse(lower.contains("edu.wpi"));
    }

    @Test
    void setpointIsImmutableAndTreatsNullUnitAsEmpty() {
        Setpoint a = new Setpoint(10.0, 2.0, "mm");
        Setpoint b = new Setpoint(10.0, 2.0, "mm");
        Setpoint c = Setpoint.at(10.0, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(10.0, c.position(), 1e-9);
        assertEquals(0.0, c.velocity(), 1e-9);
        assertEquals("", c.unitSymbol());
        assertNotEquals(a, c);
        assertEquals("mm", a.unitSymbol());
        FakeMechanismControllerAdapter adapter = new FakeMechanismControllerAdapter(0.0);
        assertThrows(NullPointerException.class, () -> adapter.effort(null, a));
        assertThrows(NullPointerException.class, () -> adapter.effort(observer().capture(), null));
    }

    private static MechanismObserver observer() {
        return MechanismObserver.builder(
                        "lift",
                        () -> 0L,
                        MechanismUnits.linearMillimeters("lift", 1.0, DirectionSign.POSITIVE))
                .ticks(() -> 100.0)
                .ticksPerSecond(() -> 0.0)
                .build();
    }

    private static boolean sessionHoldsAdapter() {
        for (Field field : MimicSession.class.getDeclaredFields()) {
            if (MechanismControllerAdapter.class.isAssignableFrom(field.getType())) {
                return true;
            }
        }
        for (Method method : MimicSession.class.getDeclaredMethods()) {
            for (Class<?> parameter : method.getParameterTypes()) {
                if (MechanismControllerAdapter.class.isAssignableFrom(parameter)) {
                    return true;
                }
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

    private static Path buildGradle() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path direct = cwd.resolve("build.gradle");
        if (Files.exists(direct)) {
            return direct;
        }
        return cwd.resolve("MIMIC").resolve("build.gradle");
    }

    /**
     * Test-only stub. Constant effort is not PID and not feedforward; it
     * exists so the seam compiles without inventing control math.
     */
    static final class FakeMechanismControllerAdapter implements MechanismControllerAdapter {
        private final double effort;

        FakeMechanismControllerAdapter(double effort) {
            this.effort = effort;
        }

        @Override
        public double effort(MechanismSnapshot snap, Setpoint setpoint) {
            if (snap == null) {
                throw new NullPointerException("snap");
            }
            if (setpoint == null) {
                throw new NullPointerException("setpoint");
            }
            return effort;
        }
    }
}
