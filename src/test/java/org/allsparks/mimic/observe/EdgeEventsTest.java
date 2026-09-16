package org.allsparks.mimic.observe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EdgeEventsTest {

    @Test
    void risingAndFallingOnKnownSamples() {
        assertTrue(EdgeEvents.rising(false, true));
        assertTrue(EdgeEvents.falling(true, false));
        assertFalse(EdgeEvents.rising(true, true));
        assertFalse(EdgeEvents.falling(false, false));
        assertFalse(EdgeEvents.rising(true, false));
        assertFalse(EdgeEvents.falling(false, true));
    }

    @Test
    void missingSamplesDoNotCreateEdges() {
        assertFalse(EdgeEvents.rising((Boolean) null, Boolean.TRUE));
        assertFalse(EdgeEvents.falling((Boolean) null, Boolean.FALSE));
        assertFalse(EdgeEvents.rising(Boolean.TRUE, null));
        assertFalse(EdgeEvents.falling(Boolean.TRUE, null));
        assertFalse(EdgeEvents.rising((Boolean) null, (Boolean) null));
        assertFalse(EdgeEvents.falling(Boolean.FALSE, Boolean.FALSE));
        assertTrue(EdgeEvents.rising(Boolean.FALSE, Boolean.TRUE));
        assertTrue(EdgeEvents.falling(Boolean.TRUE, Boolean.FALSE));
    }

    @Test
    void missingLimitSampleDoesNotLookLikeARelease() {
        LimitSwitchSample asserted = new LimitSwitchSample(
                true, true, 1L, MeasurementValidity.VALID, "lower");
        LimitSwitchSample missing = LimitSwitchSample.missing(2L, "lower");
        assertFalse(missing.asserted());
        assertFalse(EdgeEvents.falling(asserted, missing));
        assertFalse(EdgeEvents.rising(missing, asserted));

        LimitSwitchSample released = new LimitSwitchSample(
                false, false, 3L, MeasurementValidity.VALID, "lower");
        assertTrue(EdgeEvents.falling(asserted, released));
        assertTrue(EdgeEvents.rising(released, asserted));
    }
}
