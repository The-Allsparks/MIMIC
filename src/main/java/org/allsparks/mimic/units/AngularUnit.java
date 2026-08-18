package org.allsparks.mimic.units;

/** Angular units. Canonical internal unit is radians. */
public enum AngularUnit {
    RADIANS(1.0, "rad"),
    DEGREES(Math.PI / 180.0, "deg"),
    ROTATIONS(2.0 * Math.PI, "rot");

    private final double radiansPerUnit;
    private final String symbol;

    AngularUnit(double radiansPerUnit, String symbol) {
        this.radiansPerUnit = radiansPerUnit;
        this.symbol = symbol;
    }

    public double radiansPerUnit() {
        return radiansPerUnit;
    }

    public String symbol() {
        return symbol;
    }

    public double toRadians(double value) {
        return value * radiansPerUnit;
    }

    public double fromRadians(double radians) {
        return radians / radiansPerUnit;
    }
}
