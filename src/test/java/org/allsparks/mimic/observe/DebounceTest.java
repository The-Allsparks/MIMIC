package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.allsparks.mimic.clock.MimicClock;
import org.allsparks.mimic.fake.FakeActuator;
import org.junit.jupiter.api.Test;

class DebounceTest {
    /** 10 ms window. Bounce traces are spaced in nanoseconds. */
    private static final long WINDOW = 10_000_000L;
    private static final long ONE_MS = 1_000_000L;

    @Test
    void bounceShorterThanWindowIsIgnored() {
        Debounce debounce = new Debounce();
        DebounceResult settled = settle(debounce, false, 0L);
        assertEquals(Boolean.FALSE, settled.stableAsserted());
        assertFalse(settled.risingEdge());
        assertFalse(settled.fallingEdge());

        // Synthetic bounce: 1 ms asserted blip, then released again.
        DebounceResult blip = debounce.filter(true, settled.timestampNanos() + ONE_MS, WINDOW);
        assertEquals(Boolean.FALSE, blip.stableAsserted());
        assertFalse(blip.risingEdge());
        assertFalse(blip.fallingEdge());

        DebounceResult released = debounce.filter(
                false, blip.timestampNanos() + ONE_MS, WINDOW);
        assertEquals(Boolean.FALSE, released.stableAsserted());
        assertFalse(released.risingEdge());
        assertFalse(released.fallingEdge());
    }

    @Test
    void assertionHeldForWindowProducesRisingEdge() {
        Debounce debounce = new Debounce();
        DebounceResult settled = settle(debounce, false, 0L);
        long t = settled.timestampNanos() + ONE_MS;
        DebounceResult pending = debounce.filter(true, t, WINDOW);
        assertEquals(Boolean.FALSE, pending.stableAsserted());
        assertFalse(pending.risingEdge());

        DebounceResult accepted = debounce.filter(true, t + WINDOW, WINDOW);
        assertEquals(Boolean.TRUE, accepted.stableAsserted());
        assertTrue(accepted.risingEdge());
        assertFalse(accepted.fallingEdge());
    }

    @Test
    void releaseHeldForWindowProducesFallingEdge() {
        Debounce debounce = new Debounce();
        DebounceResult settled = settle(debounce, true, 0L);
        long t = settled.timestampNanos() + ONE_MS;
        debounce.filter(false, t, WINDOW);
        DebounceResult accepted = debounce.filter(false, t + WINDOW, WINDOW);
        assertEquals(Boolean.FALSE, accepted.stableAsserted());
        assertTrue(accepted.fallingEdge());
        assertFalse(accepted.risingEdge());
    }

    @Test
    void missingSamplesDoNotCreateEdges() {
        Debounce debounce = new Debounce();
        DebounceResult settled = settle(debounce, true, 0L);
        long t = settled.timestampNanos() + ONE_MS;

        DebounceResult missing = debounce.filter((Boolean) null, t, WINDOW);
        assertTrue(missing.sampleMissing());
        assertEquals(Boolean.TRUE, missing.stableAsserted());
        assertFalse(missing.risingEdge());
        assertFalse(missing.fallingEdge());

        DebounceResult stillMissing = debounce.filter(
                LimitSwitchSample.missing(t + ONE_MS, "lower"), WINDOW);
        assertTrue(stillMissing.sampleMissing());
        assertEquals(Boolean.TRUE, stillMissing.stableAsserted());
        assertFalse(stillMissing.fallingEdge());
        assertFalse(stillMissing.risingEdge());
    }

    @Test
    void missingLimitSampleDoesNotCreateAFallingEdge() {
        Debounce debounce = new Debounce();
        settle(debounce, true, 0L);
        // Factory stores asserted=false; debounce must ignore that field.
        LimitSwitchSample missing = LimitSwitchSample.missing(WINDOW + ONE_MS, "lower");
        assertFalse(missing.asserted());
        assertEquals(MeasurementValidity.MISSING, missing.validity());

        DebounceResult result = debounce.filter(missing, WINDOW);
        assertTrue(result.sampleMissing());
        assertEquals(Boolean.TRUE, result.stableAsserted());
        assertFalse(result.fallingEdge());
    }

    @Test
    void leadingMissingSamplesAreNotTreatedAsReleased() {
        Debounce debounce = new Debounce();
        DebounceResult missing = debounce.filter(
                LimitSwitchSample.missing(0L, "beam"), WINDOW);
        assertTrue(missing.sampleMissing());
        assertFalse(missing.hasStable());
        assertNull(missing.stableAsserted());
        assertFalse(missing.fallingEdge());
        assertFalse(missing.risingEdge());

        DebounceResult stillUnknown = debounce.filter(false, ONE_MS, WINDOW);
        assertFalse(stillUnknown.hasStable());
        assertFalse(stillUnknown.fallingEdge());
    }

    @Test
    void unsupportedSampleIsMissingForEdges() {
        Debounce debounce = new Debounce();
        settle(debounce, true, 0L);
        DebounceResult result = debounce.filter(
                LimitSwitchSample.unsupported(WINDOW + ONE_MS, "upper"), WINDOW);
        assertTrue(result.sampleMissing());
        assertEquals(Boolean.TRUE, result.stableAsserted());
        assertFalse(result.fallingEdge());
    }

    @Test
    void missingDuringPendingBounceDoesNotCompleteTheWindow() {
        Debounce debounce = new Debounce();
        DebounceResult settled = settle(debounce, false, 0L);
        long t = settled.timestampNanos() + ONE_MS;
        debounce.filter(true, t, WINDOW);
        debounce.filter((Boolean) null, t + (WINDOW - ONE_MS), WINDOW);
        DebounceResult afterGap = debounce.filter(true, t + WINDOW, WINDOW);
        assertEquals(Boolean.FALSE, afterGap.stableAsserted());
        assertFalse(afterGap.risingEdge());
    }

    @Test
    void syntheticBounceTraceIgnoresChatterThenAcceptsSustainedHome() {
        Debounce debounce = new Debounce();
        long t = 0L;
        DebounceResult last = settle(debounce, false, t);
        t = last.timestampNanos();

        // Chatter around a mechanical switch: T/F/T/F, each 1 ms.
        boolean[] chatter = {true, false, true, false};
        for (int i = 0; i < chatter.length; i++) {
            t += ONE_MS;
            last = debounce.filter(chatter[i], t, WINDOW);
            assertEquals(Boolean.FALSE, last.stableAsserted(), "bounce step " + i);
            assertFalse(last.risingEdge(), "bounce step " + i);
        }

        t += ONE_MS;
        debounce.filter(true, t, WINDOW);
        last = debounce.filter(true, t + WINDOW, WINDOW);
        assertEquals(Boolean.TRUE, last.stableAsserted());
        assertTrue(last.risingEdge());
    }

    @Test
    void clockOverloadUsesNanoTime() {
        Debounce debounce = new Debounce();
        AtomicLong time = new AtomicLong(0L);
        MimicClock clock = time::get;
        debounce.filter(false, clock, WINDOW);
        time.set(WINDOW);
        DebounceResult settled = debounce.filter(false, clock, WINDOW);
        assertEquals(Boolean.FALSE, settled.stableAsserted());
        assertEquals(WINDOW, settled.timestampNanos());
    }

    @Test
    void zeroWindowAcceptsImmediatelyWithoutTreatingUnknownAsReleased() {
        Debounce debounce = new Debounce();
        DebounceResult firstFalse = debounce.filter(false, 0L, 0L);
        assertEquals(Boolean.FALSE, firstFalse.stableAsserted());
        assertFalse(firstFalse.fallingEdge());

        DebounceResult rising = debounce.filter(true, 1L, 0L);
        assertEquals(Boolean.TRUE, rising.stableAsserted());
        assertTrue(rising.risingEdge());
    }

    @Test
    void negativeWindowIsRejected() {
        Debounce debounce = new Debounce();
        assertThrows(IllegalArgumentException.class, () -> debounce.filter(true, 0L, -1L));
    }

    @Test
    void debounceDoesNotWriteActuators() {
        FakeActuator actuator = new FakeActuator();
        Debounce debounce = new Debounce();
        settle(debounce, false, 0L);
        debounce.filter(true, ONE_MS, WINDOW);
        debounce.filter(LimitSwitchSample.missing(2L * ONE_MS, "lower"), WINDOW);
        assertEquals(0, actuator.outputWriteCount());
        assertEquals(0.0, actuator.power(), 1e-9);
    }

    private static DebounceResult settle(Debounce debounce, boolean asserted, long startNanos) {
        debounce.filter(asserted, startNanos, WINDOW);
        return debounce.filter(asserted, startNanos + WINDOW, WINDOW);
    }
}
