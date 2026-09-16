package org.allsparks.mimic.observe;

/**
 * Output of one {@link Debounce#filter} call.
 *
 * <p>{@link #stableAsserted()} is {@code null} until a usable sample has lasted
 * the debounce window. Null means unknown, not released.
 */
public final class DebounceResult {
    private final Boolean stableAsserted;
    private final boolean risingEdge;
    private final boolean fallingEdge;
    private final boolean sampleMissing;
    private final long timestampNanos;

    DebounceResult(
            Boolean stableAsserted,
            boolean risingEdge,
            boolean fallingEdge,
            boolean sampleMissing,
            long timestampNanos) {
        this.stableAsserted = stableAsserted;
        this.risingEdge = risingEdge;
        this.fallingEdge = fallingEdge;
        this.sampleMissing = sampleMissing;
        this.timestampNanos = timestampNanos;
    }

    /**
     * Last accepted assertion, or {@code null} if none yet.
     * Null is not the same as not-asserted.
     */
    public Boolean stableAsserted() {
        return stableAsserted;
    }

    public boolean hasStable() {
        return stableAsserted != null;
    }

    public boolean risingEdge() {
        return risingEdge;
    }

    public boolean fallingEdge() {
        return fallingEdge;
    }

    /** True when this sample was missing or otherwise unusable. */
    public boolean sampleMissing() {
        return sampleMissing;
    }

    public long timestampNanos() {
        return timestampNanos;
    }
}
