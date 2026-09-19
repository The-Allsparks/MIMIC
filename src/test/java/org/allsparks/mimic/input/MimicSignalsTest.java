package org.allsparks.mimic.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MimicSignalsTest {
    @Test
    void keysStayPerMechanism() {
        assertEquals("mimic/elev.position", MimicSignals.position("elev").qualifiedName());
        assertNotEquals(MimicSignals.position("elev"), MimicSignals.position("arm"));
        assertEquals("mimic/elev.extra.entry", MimicSignals.namedDigital("elev", "entry").qualifiedName());
    }

    @Test
    void blankNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MimicSignals.position("  "));
        assertThrows(IllegalArgumentException.class, () -> MimicSignals.namedNumeric("elev", ""));
    }

    @Test
    void currentIsDistinctFromPosition() {
        assertTrue(MimicSignals.current("elev").qualifiedName().endsWith(".current"));
        assertFalse(MimicSignals.position("elev").equals(MimicSignals.current("elev")));
    }
}
