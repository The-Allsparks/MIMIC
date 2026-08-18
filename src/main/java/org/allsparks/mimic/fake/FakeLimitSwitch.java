package org.allsparks.mimic.fake;

/** In-memory digital channel for tests. */
public final class FakeLimitSwitch {
    private volatile boolean rawState;
    private volatile boolean throwOnRead;

    public void setRawState(boolean rawState) {
        this.rawState = rawState;
    }

    public boolean rawState() {
        if (throwOnRead) {
            throw new IllegalStateException("disconnected limit switch");
        }
        return rawState;
    }

    public void disconnect() {
        this.throwOnRead = true;
    }
}
