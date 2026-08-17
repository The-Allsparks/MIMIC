package org.allsparks.mimic.observe;

/** Running min / mean / max of observer loop duration. Passive only. */
public final class LoopOverheadStats {
    private long count;
    private long totalNanos;
    private long maxNanos;
    private long minNanos = Long.MAX_VALUE;

    public void offer(long durationNanos) {
        if (durationNanos < 0L) {
            return;
        }
        count++;
        totalNanos += durationNanos;
        if (durationNanos > maxNanos) {
            maxNanos = durationNanos;
        }
        if (durationNanos < minNanos) {
            minNanos = durationNanos;
        }
    }

    public long count() {
        return count;
    }

    public long maxNanos() {
        return count == 0L ? 0L : maxNanos;
    }

    public long minNanos() {
        return count == 0L ? 0L : minNanos;
    }

    public double meanNanos() {
        if (count == 0L) {
            return Double.NaN;
        }
        return (double) totalNanos / (double) count;
    }

    public void reset() {
        count = 0L;
        totalNanos = 0L;
        maxNanos = 0L;
        minNanos = Long.MAX_VALUE;
    }
}
