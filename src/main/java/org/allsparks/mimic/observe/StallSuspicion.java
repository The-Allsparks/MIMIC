package org.allsparks.mimic.observe;

/**
 * Observe-only stall verdict. Never writes motors or servos.
 *
 * {@link #unsupported()} means current or velocity cannot evaluate the
 * heuristic (missing, NaN, or unwired). That is not stalled. Current-only
 * is not a hard limit.
 */
public final class StallSuspicion {
    public static final StallSuspicion NOT_SUSPECTED = new StallSuspicion(false, false);
    public static final StallSuspicion SUSPECTED = new StallSuspicion(true, false);
    public static final StallSuspicion UNSUPPORTED = new StallSuspicion(false, true);

    private final boolean suspected;
    private final boolean unsupported;

    private StallSuspicion(boolean suspected, boolean unsupported) {
        this.suspected = suspected;
        this.unsupported = unsupported;
    }

    /**
     * True only after high current and no motion for the detector timeout.
     * A single high-current sample is not enough.
     */
    public boolean suspected() {
        return suspected;
    }

    /**
     * True when current or velocity cannot be used. Missing current is
     * unsupported, not stalled.
     */
    public boolean unsupported() {
        return unsupported;
    }
}
