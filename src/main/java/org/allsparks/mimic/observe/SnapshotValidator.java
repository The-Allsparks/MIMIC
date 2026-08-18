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
     * disagreement is reported only when both encoders are usable, velocity is
     * {@link MeasurementValidity#UNSUPPORTED} or usable, and the observer
     * already cleared {@link MechanismSnapshot#sensorValid()} (offset above
     * threshold). Wired {@code MISSING} or {@code STALE} velocity is not
     * classified as disagreement. Range is applied only when the sample is
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
        // UNSUPPORTED velocity is optional and does not explain a false
        // sensorValid; MISSING/STALE velocity does, so those are not DISAGREEING.
        if (!snapshot.sensorValid()
                && velocityOptionalOrUsable(snapshot)
                && snapshot.redundantPosition().isUsable()) {
            return MeasurementValidity.DISAGREEING;
        }
        if (snapshot.position() < min || snapshot.position() > max) {
            return MeasurementValidity.OUT_OF_RANGE;
        }
        return MeasurementValidity.VALID;
    }

    /**
     * {@link MeasurementValidity#UNSUPPORTED} velocity is not required for
     * aggregate health. Wired {@code MISSING} or {@code STALE} velocity still
     * clears {@link MechanismSnapshot#sensorValid()} on its own, so it must
     * not be classified as encoder disagreement.
     */
    private static boolean velocityOptionalOrUsable(MechanismSnapshot snapshot) {
        return snapshot.velocitySample().validity() == MeasurementValidity.UNSUPPORTED
                || snapshot.velocitySample().isUsable();
    }
}
