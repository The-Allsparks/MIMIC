package org.allsparks.mimic.adapters.future;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SystemCoreAdapterBoundaryTest {
    @Test
    void remainsUnimplemented() {
        assertTrue(SystemCoreAdapterBoundary.STATUS.contains("UNIMPLEMENTED_BOUNDARY"));
    }
}
