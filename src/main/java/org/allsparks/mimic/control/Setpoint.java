package org.allsparks.mimic.control;

import java.util.Objects;

/**
 * Instantaneous reference a {@link MechanismControllerAdapter} tracks. May
 * differ from the operator or autonomous <em>goal</em>. Immutable; Java 11
 * (no records).
 *
 * Position and velocity use the same unit symbol as the matching
 * {@link org.allsparks.mimic.observe.MechanismSnapshot}. This type does not
 * write hardware and does not start {@code PROFILED_POSITION} motion.
 */
public final class Setpoint {
    private final double position;
    private final double velocity;
    private final String unitSymbol;

    public Setpoint(double position, double velocity, String unitSymbol) {
        this.position = position;
        this.velocity = velocity;
        this.unitSymbol = unitSymbol == null ? "" : unitSymbol;
    }

    /** Position reference with zero velocity. */
    public static Setpoint at(double position, String unitSymbol) {
        return new Setpoint(position, 0.0, unitSymbol);
    }

    public double position() {
        return position;
    }

    public double velocity() {
        return velocity;
    }

    public String unitSymbol() {
        return unitSymbol;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Setpoint)) {
            return false;
        }
        Setpoint that = (Setpoint) other;
        return Double.compare(position, that.position) == 0
                && Double.compare(velocity, that.velocity) == 0
                && unitSymbol.equals(that.unitSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, velocity, unitSymbol);
    }
}
