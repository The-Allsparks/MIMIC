package org.allsparks.mimic.observe;

/**
 * Immutable digital limit-switch observation.
 *
 * {@link #asserted()} is the interpreted "at limit" state after applying the
 * documented normally-open / normally-closed polarity. Raw hardware level is
 * {@link #rawState()}.
 */
public final class LimitSwitchSample {
    private final boolean rawState;
    private final boolean asserted;
    private final long capturedAtNanos;
    private final MeasurementValidity validity;
    private final String channelId;

    public LimitSwitchSample(
            boolean rawState,
            boolean asserted,
            long capturedAtNanos,
            MeasurementValidity validity,
            String channelId) {
        if (validity == null) {
            throw new IllegalArgumentException("validity is required");
        }
        this.rawState = rawState;
        this.asserted = asserted;
        this.capturedAtNanos = capturedAtNanos;
        this.validity = validity;
        this.channelId = channelId == null ? "" : channelId;
    }

    public static LimitSwitchSample missing(long capturedAtNanos, String channelId) {
        return new LimitSwitchSample(false, false, capturedAtNanos, MeasurementValidity.MISSING, channelId);
    }

    public static LimitSwitchSample unsupported(long capturedAtNanos, String channelId) {
        return new LimitSwitchSample(false, false, capturedAtNanos, MeasurementValidity.UNSUPPORTED, channelId);
    }

    public boolean rawState() {
        return rawState;
    }

    public boolean asserted() {
        return asserted;
    }

    /**
     * True when this channel was expected but produced no usable reading.
     * {@link #asserted()} is then {@code false} only because a boolean needs a
     * value. Missing is not "not at the limit."
     */
    public boolean missing() {
        return validity == MeasurementValidity.MISSING;
    }

    /**
     * True when no switch is wired on this channel. That is not a missing
     * reading and not a clear switch.
     */
    public boolean unsupported() {
        return validity == MeasurementValidity.UNSUPPORTED;
    }

    public long capturedAtNanos() {
        return capturedAtNanos;
    }

    public MeasurementValidity validity() {
        return validity;
    }

    public String channelId() {
        return channelId;
    }

    public boolean isUsable() {
        return validity == MeasurementValidity.VALID;
    }
}
