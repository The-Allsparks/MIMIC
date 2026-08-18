package org.allsparks.mimic.observe;

/**
 * Conservative checks on a snapshot. Does not command hardware.
 *
 * Missing or NaN measurements are classified; they are never replaced with
 * invented numbers. Position classification uses the position
 * {@link SensorSample} validity (observer-liveness {@code STALE},
 * {@code MISSING}, {@code UNSUPPORTED}) rather than reconstructing from
 * {@link MechanismSnapshot#sensorValid()}.
 */
public final class SnapshotValidator {
    private SnapshotValidator() {
    }

    public static boolean hasUsablePosition(MechanismSnapshot snapshot) {
        return snapshot != null && snapshot.positionSample().isUsable();
    }

    public static boolean effortIsFinite(double effort) {
        return !Double.isNaN(effort) && !Double.isInfinite(effort);
    }

    /**
     * Classifies the snapshot's primary position channel.
     *
     * Sample validity is returned first so {@link MeasurementValidity#STALE}
     * is distinguishable from {@link MeasurementValidity#MISSING}. Snapshot
     * disagreement is reported only when both encoders are usable and the
     * observer already cleared {@link MechanismSnapshot#sensorValid()}
     * (offset above threshold). Range is applied only when the sample is
     * {@link MeasurementValidity#VALID}.
     */
    public static MeasurementValidity classifyPosition(MechanismSnapshot snapshot, double min, double max) {
        if (snapshot == null) {
            return MeasurementValidity.MISSING;
        }
        MeasurementValidity sampleValidity = snapshot.positionSample().validity();
        if (sampleValidity != MeasurementValidity.VALID) {
            return sampleValidity;
        }
        if (Double.isNaN(snapshot.position())) {
            return MeasurementValidity.MISSING;
        }
        // Disagreement is snapshot-level: both encoders usable and the observer
        // already invalidated the aggregate (offset above threshold). A nonzero
        // offset below the threshold must not be classified as DISAGREEING.
        if (!snapshot.sensorValid()
                && snapshot.velocitySample().isUsable()
                && snapshot.redundantPosition().isUsable()) {
            return MeasurementValidity.DISAGREEING;
        }
        if (snapshot.position() < min || snapshot.position() > max) {
            return MeasurementValidity.OUT_OF_RANGE;
        }
        return MeasurementValidity.VALID;
    }
}
