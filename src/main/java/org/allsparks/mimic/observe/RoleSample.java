package org.allsparks.mimic.observe;

import java.util.Objects;

/**
 * One named or role-keyed observation. Either a {@link SensorSample} or a
 * {@link LimitSwitchSample} is populated; the other side is
 * {@link MeasurementValidity#UNSUPPORTED}.
 *
 * A missing lookup is {@code UNSUPPORTED} on both sides. That is not a fake
 * {@code false} / not-asserted digital reading and not an invented numeric
 * value.
 */
public final class RoleSample {
    private final SensorSample numeric;
    private final LimitSwitchSample digital;

    private RoleSample(SensorSample numeric, LimitSwitchSample digital) {
        this.numeric = Objects.requireNonNull(numeric, "numeric");
        this.digital = Objects.requireNonNull(digital, "digital");
    }

    public static RoleSample of(SensorSample numeric) {
        Objects.requireNonNull(numeric, "numeric");
        return new RoleSample(
                numeric, LimitSwitchSample.unsupported(numeric.capturedAtNanos(), numeric.channelId()));
    }

    public static RoleSample of(LimitSwitchSample digital) {
        Objects.requireNonNull(digital, "digital");
        return new RoleSample(
                SensorSample.unsupported(digital.capturedAtNanos(), digital.channelId(), ""), digital);
    }

    public static RoleSample unsupported(long capturedAtNanos, String channelId) {
        return new RoleSample(
                SensorSample.unsupported(capturedAtNanos, channelId, ""),
                LimitSwitchSample.unsupported(capturedAtNanos, channelId));
    }

    public SensorSample numeric() {
        return numeric;
    }

    public LimitSwitchSample digital() {
        return digital;
    }

    /** True when this observation is a numeric channel, including {@code MISSING}. */
    public boolean hasNumeric() {
        return numeric.validity() != MeasurementValidity.UNSUPPORTED;
    }

    /** True when this observation is a digital channel, including {@code MISSING}. */
    public boolean hasDigital() {
        return digital.validity() != MeasurementValidity.UNSUPPORTED;
    }
}
