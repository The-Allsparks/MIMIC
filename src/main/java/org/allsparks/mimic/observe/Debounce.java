package org.allsparks.mimic.observe;

import org.allsparks.mimic.clock.MimicClock;

/**
 * Requires a digital input to stay the same for a time window before the
 * filtered output changes.
 *
 * <p>A one-loop blip is not a home: a bounce shorter than {@code windowNanos}
 * is ignored. A missing sample ({@code null} asserted, or an unusable
 * {@link LimitSwitchSample}) is not treated as not-asserted and does not
 * create a rising or falling edge.
 *
 * <p>Pass timestamps from {@link MimicClock#nanoTime()} or any equivalent
 * {@code long nowNanos}. This helper never writes motors or servos.
 */
public final class Debounce {
    private Boolean stableAsserted;
    private Boolean pendingAsserted;
    private long pendingSinceNanos;

    /**
     * Known-valid sample. Use {@link #filter(Boolean, long, long)} with
     * {@code null} when the sample is missing.
     */
    public DebounceResult filter(boolean asserted, long nowNanos, long windowNanos) {
        return filter(Boolean.valueOf(asserted), nowNanos, windowNanos);
    }

    /**
     * Same as {@link #filter(Boolean, long, long)} using {@link MimicClock}.
     */
    public DebounceResult filter(Boolean asserted, MimicClock clock, long windowNanos) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        return filter(asserted, clock.nanoTime(), windowNanos);
    }

    /**
     * Unusable samples (missing, unsupported, stale) are ignored for edges.
     * Do not pass {@link LimitSwitchSample#asserted()} from a missing sample;
     * that field is {@code false} and would look like a release.
     */
    public DebounceResult filter(LimitSwitchSample sample, long windowNanos) {
        if (sample == null) {
            throw new IllegalArgumentException("sample is required");
        }
        Boolean asserted = sample.isUsable() ? Boolean.valueOf(sample.asserted()) : null;
        return filter(asserted, sample.capturedAtNanos(), windowNanos);
    }

    /**
     * @param asserted {@code true}/{@code false} when usable; {@code null} if missing
     * @param nowNanos timestamp from {@link MimicClock#nanoTime()}
     * @param windowNanos how long the sample must stay unchanged; {@code 0} accepts immediately
     */
    public DebounceResult filter(Boolean asserted, long nowNanos, long windowNanos) {
        if (windowNanos < 0L) {
            throw new IllegalArgumentException("windowNanos must be >= 0");
        }
        if (asserted == null) {
            pendingAsserted = null;
            return new DebounceResult(stableAsserted, false, false, true, nowNanos);
        }
        if (stableAsserted != null && asserted.equals(stableAsserted)) {
            pendingAsserted = null;
            return new DebounceResult(stableAsserted, false, false, false, nowNanos);
        }
        if (pendingAsserted == null || !asserted.equals(pendingAsserted)) {
            pendingAsserted = asserted;
            pendingSinceNanos = nowNanos;
        }
        if (nowNanos - pendingSinceNanos < windowNanos) {
            return new DebounceResult(stableAsserted, false, false, false, nowNanos);
        }
        Boolean previousStable = stableAsserted;
        stableAsserted = asserted;
        pendingAsserted = null;
        boolean rising = Boolean.TRUE.equals(stableAsserted) && Boolean.FALSE.equals(previousStable);
        boolean falling = Boolean.FALSE.equals(stableAsserted) && Boolean.TRUE.equals(previousStable);
        return new DebounceResult(stableAsserted, rising, falling, false, nowNanos);
    }

    /** Clears stable and pending state. Does not write hardware. */
    public void reset() {
        stableAsserted = null;
        pendingAsserted = null;
        pendingSinceNanos = 0L;
    }
}
