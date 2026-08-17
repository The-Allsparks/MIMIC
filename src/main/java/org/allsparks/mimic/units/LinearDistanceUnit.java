package org.allsparks.mimic.units;

/** Linear distance units. Canonical internal unit is millimeters. */
public enum LinearDistanceUnit {
    MILLIMETERS(1.0, "mm"),
    METERS(1000.0, "m"),
    INCHES(25.4, "in");

    private final double millimetersPerUnit;
    private final String symbol;

    LinearDistanceUnit(double millimetersPerUnit, String symbol) {
        this.millimetersPerUnit = millimetersPerUnit;
        this.symbol = symbol;
    }

    public double millimetersPerUnit() {
        return millimetersPerUnit;
    }

    public String symbol() {
        return symbol;
    }

    public double toMillimeters(double value) {
        return value * millimetersPerUnit;
    }

    public double fromMillimeters(double millimeters) {
        return millimeters / millimetersPerUnit;
    }
}
