package org.allsparks.mimic.input;

import java.util.Objects;
import org.allsparks.contracts.input.InputValues;
import org.allsparks.contracts.input.Sample;
import org.allsparks.contracts.input.SignalKey;
import org.allsparks.contracts.observation.Validity;
import org.allsparks.mimic.observe.LimitSwitchSample;
import org.allsparks.mimic.observe.MeasurementValidity;
import org.allsparks.mimic.observe.SensorSample;

/**
 * Reads published {@link InputValues} without calling hardware getters.
 *
 * <p>MIMIC still does not depend on PULSE. Invalid / missing contracts samples
 * become {@link MeasurementValidity#MISSING}, never invented zeros or a fake
 * clear limit switch.
 */
public final class InputValuesReads {
    private InputValuesReads() {}

    public static SensorSample numeric(
            InputValues values, SignalKey<Double> key, long nowNanos, String channelId, String unitSymbol) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(key, "key");
        Sample<Double> sample = values.get(key);
        MeasurementValidity validity = mapValidity(sample.validity());
        Double boxed = sample.orNull();
        long capturedAt = sample.captureTimestampNanos() > 0L ? sample.captureTimestampNanos() : nowNanos;
        if (boxed == null || Double.isNaN(boxed.doubleValue())) {
            if (validity == MeasurementValidity.VALID) {
                validity = MeasurementValidity.MISSING;
            }
            return new SensorSample(Double.NaN, capturedAt, validity, channelId, unitSymbol);
        }
        return new SensorSample(boxed.doubleValue(), capturedAt, validity, channelId, unitSymbol);
    }

    public static LimitSwitchSample digital(
            InputValues values,
            SignalKey<Boolean> key,
            boolean inverted,
            long nowNanos,
            String channelId) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(key, "key");
        Sample<Boolean> sample = values.get(key);
        MeasurementValidity validity = mapValidity(sample.validity());
        Boolean boxed = sample.orNull();
        long capturedAt = sample.captureTimestampNanos() > 0L ? sample.captureTimestampNanos() : nowNanos;
        if (validity != MeasurementValidity.VALID && validity != MeasurementValidity.STALE) {
            if (validity == MeasurementValidity.UNSUPPORTED) {
                return LimitSwitchSample.unsupported(capturedAt, channelId);
            }
            return LimitSwitchSample.missing(capturedAt, channelId);
        }
        if (boxed == null) {
            return LimitSwitchSample.missing(capturedAt, channelId);
        }
        boolean raw = boxed.booleanValue();
        boolean asserted = inverted != raw;
        return new LimitSwitchSample(raw, asserted, capturedAt, validity, channelId);
    }

    static MeasurementValidity mapValidity(Validity validity) {
        if (validity == null) {
            return MeasurementValidity.MISSING;
        }
        switch (validity) {
            case VALID:
                return MeasurementValidity.VALID;
            case STALE:
                return MeasurementValidity.STALE;
            case OUT_OF_RANGE:
                return MeasurementValidity.OUT_OF_RANGE;
            case UNSUPPORTED:
                return MeasurementValidity.UNSUPPORTED;
            case MISSING:
            case INVALID:
            default:
                return MeasurementValidity.MISSING;
        }
    }
}
