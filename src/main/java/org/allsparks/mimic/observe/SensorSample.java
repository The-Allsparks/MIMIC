package org.allsparks.mimic.observe;

/** Immutable numeric sample with capture time, validity, and documented units. */
public final class SensorSample {
    private final double value;
    private final long capturedAtNanos;
    private final MeasurementValidity validity;
    private final String channelId;
    private final String unitSymbol;

    public SensorSample(
            double value,
            long capturedAtNanos,
            MeasurementValidity validity,
            String channelId,
            String unitSymbol) {
        if (validity == null) {
            throw new IllegalArgumentException("validity is required");
        }
        this.value = value;
        this.capturedAtNanos = capturedAtNanos;
        this.validity = validity;
        this.channelId = channelId == null ? "" : channelId;
        this.unitSymbol = unitSymbol == null ? "" : unitSymbol;
    }

    public static SensorSample missing(long capturedAtNanos, String channelId, String unitSymbol) {
        return new SensorSample(Double.NaN, capturedAtNanos, MeasurementValidity.MISSING, channelId, unitSymbol);
    }

    public static SensorSample unsupported(long capturedAtNanos, String channelId, String unitSymbol) {
        return new SensorSample(Double.NaN, capturedAtNanos, MeasurementValidity.UNSUPPORTED, channelId, unitSymbol);
    }

    public static SensorSample stale(double value, long capturedAtNanos, String channelId, String unitSymbol) {
        return new SensorSample(value, capturedAtNanos, MeasurementValidity.STALE, channelId, unitSymbol);
    }

    public double value() {
        return value;
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

    public String unitSymbol() {
        return unitSymbol;
    }

    public boolean isUsable() {
        return validity == MeasurementValidity.VALID && !Double.isNaN(value);
    }
}
