package org.allsparks.mimic.clock;

/** Production clock backed by {@link System#nanoTime()}. */
public final class SystemNanoClock implements MimicClock {
    @Override
    public long nanoTime() {
        return System.nanoTime();
    }
}
