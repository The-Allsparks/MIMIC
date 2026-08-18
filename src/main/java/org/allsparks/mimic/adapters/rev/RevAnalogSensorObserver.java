package org.allsparks.mimic.adapters.rev;

import java.util.Objects;
import java.util.function.DoubleSupplier;
import org.allsparks.mimic.observe.MeasurementValidity;
import org.allsparks.mimic.observe.SensorSample;

/**
 * Passive analog or absolute-sensor adapter (for example a potentiometer or
 * analog absolute encoder wired to a REV analog port).
 *
 * This adapter never commands hardware. Voltage-to-angle conversion is the
 * caller's documented mapping; this class only captures the supplied value.
 */
public final class RevAnalogSensorObserver {
    private final String channelId;
    private final DoubleSupplier value;
    private final String unitSymbol;

    public RevAnalogSensorObserver(String channelId, DoubleSupplier value, String unitSymbol) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId must be non-empty");
        }
        this.channelId = channelId;
        this.value = Objects.requireNonNull(value, "value");
        this.unitSymbol = unitSymbol == null ? "" : unitSymbol;
    }

    public SensorSample read(long nowNanos) {
        try {
            double sample = value.getAsDouble();
            if (Double.isNaN(sample)) {
                return SensorSample.missing(nowNanos, channelId, unitSymbol);
            }
            return new SensorSample(sample, nowNanos, MeasurementValidity.VALID, channelId, unitSymbol);
        } catch (RuntimeException ex) {
            return SensorSample.missing(nowNanos, channelId, unitSymbol);
        }
    }

    public String channelId() {
        return channelId;
    }
}
