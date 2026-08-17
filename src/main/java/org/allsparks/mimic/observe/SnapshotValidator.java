package org.allsparks.mimic.observe;

/**
 * Conservative checks on a snapshot. Does not command hardware.
 *
 * Missing or NaN measurements are classified; they are never replaced with
 * invented numbers.
 */
public final class SnapshotValidator {
    private SnapshotValidator() {
    }

    public static boolean hasUsablePosition(MechanismSnapshot snapshot) {
        return snapshot != null && snapshot.sensorValid() && !Double.isNaN(snapshot.position());
    }

    public static boolean effortIsFinite(double effort) {
        return !Double.isNaN(effort) && !Double.isInfinite(effort);
    }

    public static MeasurementValidity classifyPosition(MechanismSnapshot snapshot, double min, double max) {
        if (snapshot == null) {
            return MeasurementValidity.MISSING;
        }
        if (Double.isNaN(snapshot.position())) {
            return MeasurementValidity.MISSING;
        }
        if (!snapshot.sensorValid()) {
            if (!Double.isNaN(snapshot.disagreement()) && snapshot.disagreement() > 0.0) {
                return MeasurementValidity.DISAGREEING;
            }
            return MeasurementValidity.MISSING;
        }
        if (snapshot.position() < min || snapshot.position() > max) {
            return MeasurementValidity.OUT_OF_RANGE;
        }
        return MeasurementValidity.VALID;
    }
}
