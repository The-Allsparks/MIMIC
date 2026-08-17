package org.allsparks.mimic.units;

import java.util.Objects;

/**
 * Documented conversion from encoder ticks to mechanism coordinates.
 *
 * Incorrect gearing or direction can command a mechanism into a hard stop.
 * Phase 0 only converts observations; it never writes motors.
 */
public final class MechanismUnits {
    private final double ticksPerCanonicalUnit;
    private final DirectionSign direction;
    private final String canonicalUnitSymbol;
    private final String mechanismId;

    public MechanismUnits(
            String mechanismId,
            double ticksPerCanonicalUnit,
            DirectionSign direction,
            String canonicalUnitSymbol) {
        if (mechanismId == null || mechanismId.isEmpty()) {
            throw new IllegalArgumentException("mechanismId must be non-empty");
        }
        if (!(ticksPerCanonicalUnit > 0.0) || Double.isNaN(ticksPerCanonicalUnit)) {
            throw new IllegalArgumentException("ticksPerCanonicalUnit must be finite and > 0");
        }
        if (canonicalUnitSymbol == null || canonicalUnitSymbol.isEmpty()) {
            throw new IllegalArgumentException("canonicalUnitSymbol must be non-empty");
        }
        this.mechanismId = mechanismId;
        this.ticksPerCanonicalUnit = ticksPerCanonicalUnit;
        this.direction = Objects.requireNonNull(direction, "direction");
        this.canonicalUnitSymbol = canonicalUnitSymbol;
    }

    public static MechanismUnits linearMillimeters(String mechanismId, double ticksPerMillimeter, DirectionSign direction) {
        return new MechanismUnits(mechanismId, ticksPerMillimeter, direction, LinearDistanceUnit.MILLIMETERS.symbol());
    }

    public static MechanismUnits rotaryRadians(String mechanismId, double ticksPerRadian, DirectionSign direction) {
        return new MechanismUnits(mechanismId, ticksPerRadian, direction, AngularUnit.RADIANS.symbol());
    }

    public String mechanismId() {
        return mechanismId;
    }

    public double ticksPerCanonicalUnit() {
        return ticksPerCanonicalUnit;
    }

    public DirectionSign direction() {
        return direction;
    }

    public String canonicalUnitSymbol() {
        return canonicalUnitSymbol;
    }

    public double ticksToCanonical(double ticks) {
        return (ticks / ticksPerCanonicalUnit) * direction.multiplier();
    }

    public double canonicalToTicks(double canonical) {
        return (canonical / direction.multiplier()) * ticksPerCanonicalUnit;
    }

    public double ticksPerSecondToCanonical(double ticksPerSecond) {
        return ticksToCanonical(ticksPerSecond);
    }
}
