package org.allsparks.mimic.units;

/**
 * Sign applied after converting encoder ticks to mechanism units.
 *
 * {@code POSITIVE} means increasing encoder ticks increase the documented
 * mechanism coordinate. {@code NEGATIVE} inverts that mapping.
 */
public enum DirectionSign {
    POSITIVE(1.0),
    NEGATIVE(-1.0);

    private final double multiplier;

    DirectionSign(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    public DirectionSign inverted() {
        return this == POSITIVE ? NEGATIVE : POSITIVE;
    }
}
