package org.allsparks.mimic.adapters.rev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.allsparks.mimic.observe.LimitSwitchSample;
import org.allsparks.mimic.observe.MeasurementValidity;
import org.junit.jupiter.api.Test;

class RevDigitalChannelObserverTest {

    @Test
    void normallyOpenTrueIsAsserted() {
        RevDigitalChannelObserver observer = new RevDigitalChannelObserver("lower", () -> true, false);
        LimitSwitchSample sample = observer.read(1L);
        assertTrue(sample.rawState());
        assertTrue(sample.asserted());
        assertEquals(MeasurementValidity.VALID, sample.validity());
    }

    @Test
    void normallyClosedFalseIsAsserted() {
        RevDigitalChannelObserver observer = new RevDigitalChannelObserver("lower", () -> false, true);
        LimitSwitchSample sample = observer.read(1L);
        assertFalse(sample.rawState());
        assertTrue(sample.asserted());
    }

    @Test
    void exceptionBecomesMissing() {
        RevDigitalChannelObserver observer = new RevDigitalChannelObserver("lower", () -> {
            throw new IllegalStateException("disconnected");
        }, false);
        LimitSwitchSample sample = observer.read(2L);
        assertEquals(MeasurementValidity.MISSING, sample.validity());
        assertFalse(sample.asserted());
    }
}
