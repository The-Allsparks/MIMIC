package org.allsparks.mimic.fake;

/** In-memory analog/absolute sensor for tests. */
public final class FakeAbsoluteSensor {
    private volatile double value = Double.NaN;
    private volatile boolean throwOnRead;

    public void setValue(double value) {
        this.value = value;
    }

    public double value() {
        if (throwOnRead) {
            throw new IllegalStateException("disconnected absolute sensor");
        }
        return value;
    }

    public void disconnect() {
        this.throwOnRead = true;
    }
}
