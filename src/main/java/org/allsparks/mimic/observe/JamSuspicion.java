package org.allsparks.mimic.observe;

/**
 * Observe-only jam verdict. Same heuristic as {@link StallSuspicion}:
 * current plus no motion for a timeout. Never reverse-clears and never
 * writes hardware.
 */
public final class JamSuspicion {
    public static final JamSuspicion NOT_SUSPECTED = new JamSuspicion(false, false);
    public static final JamSuspicion SUSPECTED = new JamSuspicion(true, false);
    public static final JamSuspicion UNSUPPORTED = new JamSuspicion(false, true);

    private final boolean suspected;
    private final boolean unsupported;

    private JamSuspicion(boolean suspected, boolean unsupported) {
        this.suspected = suspected;
        this.unsupported = unsupported;
    }

    /** Map a stall verdict onto the jam name. The heuristic is the same. */
    public static JamSuspicion from(StallSuspicion stall) {
        if (stall == null) {
            throw new NullPointerException("stall");
        }
        if (stall.unsupported()) {
            return UNSUPPORTED;
        }
        if (stall.suspected()) {
            return SUSPECTED;
        }
        return NOT_SUSPECTED;
    }

    /**
     * True only after high current and no motion for the detector timeout.
     * Not permission to reverse-clear.
     */
    public boolean suspected() {
        return suspected;
    }

    /**
     * True when current or velocity cannot be used. Missing current is
     * unsupported, not jammed.
     */
    public boolean unsupported() {
        return unsupported;
    }
}
