package org.allsparks.mimic.input;

import org.allsparks.contracts.input.SignalKey;

/**
 * Mechanism {@link SignalKey} identities MIMIC declares on an
 * {@link org.allsparks.contracts.input.InputRegistrar}.
 *
 * <p>These keys are the contracts SPI, not PULSE types. A sampler such as PULSE
 * may implement the registrar. MIMIC still compiles and runs without PULSE:
 * {@link org.allsparks.mimic.observe.MechanismObserver} keeps raw
 * {@code DoubleSupplier} / {@code BooleanSupplier} paths when no registrar is
 * supplied.
 *
 * <p>MIMIC's own {@link org.allsparks.mimic.observe.MeasurementValidity#STALE}
 * remains observer-liveness (gap between {@code capture()} calls). Contracts
 * capture cadence is a different type.
 */
public final class MimicSignals {
    private MimicSignals() {}

    public static SignalKey<Double> position(String mechanismId) {
        return SignalKey.doubleKey("mimic", property(mechanismId, "position"));
    }

    public static SignalKey<Double> velocity(String mechanismId) {
        return SignalKey.doubleKey("mimic", property(mechanismId, "velocity"));
    }

    public static SignalKey<Double> current(String mechanismId) {
        return SignalKey.doubleKey("mimic", property(mechanismId, "current"));
    }

    public static SignalKey<Boolean> lowerLimit(String mechanismId) {
        return SignalKey.booleanKey("mimic", property(mechanismId, "lowerLimit"));
    }

    public static SignalKey<Boolean> upperLimit(String mechanismId) {
        return SignalKey.booleanKey("mimic", property(mechanismId, "upperLimit"));
    }

    public static SignalKey<Double> absolute(String mechanismId) {
        return SignalKey.doubleKey("mimic", property(mechanismId, "absolute"));
    }

    public static SignalKey<Double> redundant(String mechanismId) {
        return SignalKey.doubleKey("mimic", property(mechanismId, "redundant"));
    }

    public static SignalKey<Double> namedNumeric(String mechanismId, String extraName) {
        return SignalKey.doubleKey("mimic", property(mechanismId, "extra." + requireName(extraName)));
    }

    public static SignalKey<Boolean> namedDigital(String mechanismId, String extraName) {
        return SignalKey.booleanKey("mimic", property(mechanismId, "extra." + requireName(extraName)));
    }

    private static String property(String mechanismId, String channel) {
        return requireName(mechanismId) + "." + channel;
    }

    private static String requireName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return trimmed;
    }
}
