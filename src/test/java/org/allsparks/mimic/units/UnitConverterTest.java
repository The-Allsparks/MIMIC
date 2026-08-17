package org.allsparks.mimic.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UnitConverterTest {
    @Test
    void millimetersToInches() {
        assertEquals(1.0, UnitConverter.linear(25.4, LinearDistanceUnit.MILLIMETERS, LinearDistanceUnit.INCHES), 1e-9);
    }

    @Test
    void degreesToRadians() {
        assertEquals(Math.PI, UnitConverter.angular(180.0, AngularUnit.DEGREES, AngularUnit.RADIANS), 1e-12);
    }

    @Test
    void encoderDirectionInvertsCanonicalPosition() {
        MechanismUnits positive = MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.POSITIVE);
        MechanismUnits negative = MechanismUnits.linearMillimeters("elev", 10.0, DirectionSign.NEGATIVE);
        assertEquals(5.0, positive.ticksToCanonical(50.0), 1e-9);
        assertEquals(-5.0, negative.ticksToCanonical(50.0), 1e-9);
        assertEquals(50.0, positive.canonicalToTicks(5.0), 1e-9);
        assertEquals(50.0, negative.canonicalToTicks(-5.0), 1e-9);
    }

    @Test
    void rejectsNonPositiveGearing() {
        assertThrows(IllegalArgumentException.class,
                () -> MechanismUnits.linearMillimeters("elev", 0.0, DirectionSign.POSITIVE));
    }
}
