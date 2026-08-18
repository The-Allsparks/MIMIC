package org.allsparks.mimic.adapters.rev;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.allsparks.mimic.observe.LimitSwitchSample;
import org.allsparks.mimic.observe.MeasurementValidity;

/**
 * Passive REV digital-channel adapter for limit switches.
 *
 * On-robot, wire {@code DigitalChannel#getState()}. This adapter never writes
 * the channel mode or motor output.
 *
 * {@code inverted} is true when the raw {@code true} level means the switch is
 * <em>not</em> asserted (typical normally-closed wiring).
 */
public final class RevDigitalChannelObserver {
    private final String channelId;
    private final BooleanSupplier rawState;
    private final boolean inverted;

    public RevDigitalChannelObserver(String channelId, BooleanSupplier rawState, boolean inverted) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId must be non-empty");
        }
        this.channelId = channelId;
        this.rawState = Objects.requireNonNull(rawState, "rawState");
        this.inverted = inverted;
    }

    public LimitSwitchSample read(long nowNanos) {
        try {
            boolean raw = rawState.getAsBoolean();
            boolean asserted = inverted != raw;
            return new LimitSwitchSample(raw, asserted, nowNanos, MeasurementValidity.VALID, channelId);
        } catch (RuntimeException ex) {
            return LimitSwitchSample.missing(nowNanos, channelId);
        }
    }

    public String channelId() {
        return channelId;
    }
}
