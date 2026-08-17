package org.allsparks.mimic.adapters.amper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class AmperBoundaryTest {

    @Test
    void unrestrictedGrantDoesNotAlterRequestedEffort() {
        AmperPowerGrant grant = AmperPowerGrant.unrestricted(0.42);
        assertEquals(0.42, grant.allowedEffort(), 1e-9);
        assertFalse(grant.delayed());
        assertEquals("FEATURE_DISABLED", grant.reason());
        assertEquals(1.0, grant.confidence(), 1e-9);
    }

    @Test
    void requestPreservesGravityCriticalHold() {
        AmperPowerRequest request = new AmperPowerRequest(
                "elevator", 0.3, 4.0, 0.12, true, false, 0L, 500_000_000L, "HOLD");
        assertEquals(0.12, request.safeMinimumHoldingEffort(), 1e-9);
        assertEquals(true, request.gravityCritical());
        assertEquals("HOLD", request.motionPhase());
    }
}
