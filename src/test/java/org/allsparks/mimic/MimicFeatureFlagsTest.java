package org.allsparks.mimic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MimicFeatureFlagsTest {

    @Test
    void defaultsEnableOnlyPhase0() {
        MimicFeatureFlags flags = MimicFeatureFlags.defaults();
        assertTrue(flags.isPhase0Contracts());
        assertFalse(flags.isPhase1PassiveObservation());
        assertFalse(flags.isAnyActuationEnabled());
    }

    @Test
    void passiveObservationDoesNotActuate() {
        MimicFeatureFlags flags = MimicFeatureFlags.passiveObservation();
        assertTrue(flags.isPhase1PassiveObservation());
        assertFalse(flags.isAnyActuationEnabled());
    }
}
