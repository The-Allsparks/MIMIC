package org.allsparks.mimic.units;

/** Stateless conversion helpers for documented mechanism units. */
public final class UnitConverter {
    private UnitConverter() {
    }

    public static double linear(double value, LinearDistanceUnit from, LinearDistanceUnit to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("units are required");
        }
        return to.fromMillimeters(from.toMillimeters(value));
    }

    public static double angular(double value, AngularUnit from, AngularUnit to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("units are required");
        }
        return to.fromRadians(from.toRadians(value));
    }
}
