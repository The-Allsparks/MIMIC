package org.allsparks.mimic.observe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Observe-only occupancy, identity, and count tracker. Sketch:
 * {@code PieceTracker.capacity(3).identitySlot(0, teamProvidedLabel)}.
 *
 * Reuses {@link PieceObservation#from(MechanismSnapshot)}. Missing or
 * unwired sensors are unknown occupancy, not empty: this type does not
 * invent a count. Entry vs count disagreement is
 * {@link MeasurementValidity#DISAGREEING} and also unknown. Identity
 * labels are TeamCode-provided strings, not season enums.
 *
 * Unused by {@code MimicSession}. Does not debounce (that lives elsewhere),
 * does not auto-spit, does not write motors or servos, and does not own
 * vision. Reject routing that would run motors stays later.
 */
public final class PieceTracker {
    private static final double WHOLE_EPS = 1e-9;
    private static final double SINGLE_SOURCE_CONFIDENCE = 0.5;
    private static final double AGREED_CONFIDENCE = 1.0;

    private final int capacity;
    private final String[] labels;

    private PieceTracker(int capacity, String[] labels) {
        this.capacity = capacity;
        this.labels = labels;
    }

    /**
     * Tracker with {@code capacity} pockets and no identity labels yet.
     * Capacity is pocket count, not occupancy: an unwired robot stays
     * unknown, not empty.
     *
     * @param capacity pocket count; must be {@code >= 1}
     */
    public static PieceTracker capacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        return new PieceTracker(capacity, new String[capacity]);
    }

    /**
     * TeamCode identity string for one pocket. Not a season enum. Blank
     * labels are rejected so unknown stays a first-class state instead of
     * an empty name.
     */
    public PieceTracker identitySlot(int index, String teamProvidedLabel) {
        requireSlot(index);
        Objects.requireNonNull(teamProvidedLabel, "teamProvidedLabel");
        String trimmed = teamProvidedLabel.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                    "identity label must be a TeamCode-provided string");
        }
        String[] next = Arrays.copyOf(labels, labels.length);
        next[index] = trimmed;
        return new PieceTracker(capacity, next);
    }

    public int capacity() {
        return capacity;
    }

    /**
     * Configured TeamCode label for {@code slot}, or empty when that
     * pocket has no label yet. This is declaration, not occupancy.
     */
    public String identityLabel(int slot) {
        requireSlot(slot);
        return labels[slot] == null ? "" : labels[slot];
    }

    /**
     * Always false. A tracker report is not permission to run an intake
     * reverse, spit, or reject path.
     */
    public boolean permitsMotion() {
        return false;
    }

    /**
     * Fold {@link PieceObservation#from(MechanismSnapshot)} into occupancy,
     * count, identity, and confidence. Reads sensors only; does not write
     * hardware.
     */
    public Report observe(MechanismSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return reconcile(PieceObservation.from(snapshot));
    }

    /**
     * Reconcile entry/exit/count evidence. Disagreement becomes unknown
     * occupancy and does not invent a count.
     */
    public Report reconcile(PieceObservation observation) {
        Objects.requireNonNull(observation, "observation");

        Boolean entryPresent = knownPresent(observation.entry());
        Boolean exitPresent = knownPresent(observation.exit());
        Integer countValue = knownCount(observation.count());
        boolean countOverflow = countOverCapacity(observation.count());

        boolean disagreed = countOverflow;
        if (entryPresent != null && countValue != null) {
            boolean countSaysPresent = countValue.intValue() > 0;
            if (entryPresent.booleanValue() != countSaysPresent) {
                disagreed = true;
            }
        }
        if (Boolean.TRUE.equals(exitPresent) && countValue != null && countValue.intValue() == 0) {
            disagreed = true;
        }

        if (disagreed) {
            return Report.unknown(
                    observation,
                    capacity,
                    labels,
                    MeasurementValidity.DISAGREEING,
                    0.0);
        }

        if (countValue != null) {
            boolean present = countValue.intValue() > 0;
            double confidence = entryPresent != null ? AGREED_CONFIDENCE : SINGLE_SOURCE_CONFIDENCE;
            return Report.known(
                    observation,
                    capacity,
                    labels,
                    present,
                    countValue.intValue(),
                    confidence);
        }

        if (Boolean.TRUE.equals(entryPresent) || Boolean.TRUE.equals(exitPresent)) {
            return Report.knownPresentCountUnknown(
                    observation, capacity, labels, SINGLE_SOURCE_CONFIDENCE);
        }

        return Report.unknown(
                observation,
                capacity,
                labels,
                unknownValidity(observation),
                0.0);
    }

    private void requireSlot(int index) {
        if (index < 0 || index >= capacity) {
            throw new IllegalArgumentException("identity slot index out of range");
        }
    }

    private static Boolean knownPresent(PieceObservation.Presence presence) {
        if (presence.isUnknown()) {
            return null;
        }
        return Boolean.valueOf(presence.present());
    }

    private Integer knownCount(PieceObservation.Count count) {
        if (count.isUnknown() || !isWholeNonNegative(count.value())) {
            return null;
        }
        int whole = (int) Math.rint(count.value());
        if (whole > capacity) {
            return null;
        }
        return Integer.valueOf(whole);
    }

    private boolean countOverCapacity(PieceObservation.Count count) {
        if (count.isUnknown() || !isWholeNonNegative(count.value())) {
            return false;
        }
        return (int) Math.rint(count.value()) > capacity;
    }

    private static boolean isWholeNonNegative(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return false;
        }
        return Math.abs(value - Math.rint(value)) < WHOLE_EPS;
    }

    private static MeasurementValidity unknownValidity(PieceObservation observation) {
        MeasurementValidity entry = observation.entry().validity();
        MeasurementValidity exit = observation.exit().validity();
        MeasurementValidity count = observation.count().validity();
        if (entry == MeasurementValidity.MISSING
                || exit == MeasurementValidity.MISSING
                || count == MeasurementValidity.MISSING) {
            return MeasurementValidity.MISSING;
        }
        if (entry == MeasurementValidity.UNSUPPORTED
                && exit == MeasurementValidity.UNSUPPORTED
                && count == MeasurementValidity.UNSUPPORTED) {
            return MeasurementValidity.UNSUPPORTED;
        }
        return MeasurementValidity.UNSUPPORTED;
    }

    /**
     * Reconciled occupancy for one snapshot. {@link #present()} and
     * {@link #count()} are only claims when {@link #occupancyUnknown()} is
     * false. Unknown is first-class: do not treat a false present bit or a
     * NaN count as empty.
     */
    public static final class Report {
        private final PieceObservation observation;
        private final int capacity;
        private final String[] labels;
        private final boolean occupancyUnknown;
        private final boolean present;
        private final boolean countUnknown;
        private final double count;
        private final MeasurementValidity occupancyValidity;
        private final double confidence;

        private Report(
                PieceObservation observation,
                int capacity,
                String[] labels,
                boolean occupancyUnknown,
                boolean present,
                boolean countUnknown,
                double count,
                MeasurementValidity occupancyValidity,
                double confidence) {
            this.observation = observation;
            this.capacity = capacity;
            this.labels = labels;
            this.occupancyUnknown = occupancyUnknown;
            this.present = present;
            this.countUnknown = countUnknown;
            this.count = count;
            this.occupancyValidity = occupancyValidity;
            this.confidence = confidence;
        }

        static Report unknown(
                PieceObservation observation,
                int capacity,
                String[] labels,
                MeasurementValidity validity,
                double confidence) {
            return new Report(
                    observation,
                    capacity,
                    labels,
                    true,
                    false,
                    true,
                    Double.NaN,
                    validity,
                    confidence);
        }

        static Report known(
                PieceObservation observation,
                int capacity,
                String[] labels,
                boolean present,
                int count,
                double confidence) {
            return new Report(
                    observation,
                    capacity,
                    labels,
                    false,
                    present,
                    false,
                    count,
                    MeasurementValidity.VALID,
                    confidence);
        }

        static Report knownPresentCountUnknown(
                PieceObservation observation,
                int capacity,
                String[] labels,
                double confidence) {
            return new Report(
                    observation,
                    capacity,
                    labels,
                    false,
                    true,
                    true,
                    Double.NaN,
                    MeasurementValidity.VALID,
                    confidence);
        }

        public PieceObservation observation() {
            return observation;
        }

        public int capacity() {
            return capacity;
        }

        /**
         * True when path occupancy cannot be claimed. Zero sensors and
         * entry-vs-count disagreement both land here; neither is empty.
         */
        public boolean occupancyUnknown() {
            return occupancyUnknown;
        }

        /**
         * Interpreted present state. Meaningful only when
         * {@link #occupancyUnknown()} is false. False here is not empty
         * occupancy when the report is unknown.
         */
        public boolean present() {
            return present;
        }

        public boolean isKnownPresent() {
            return !occupancyUnknown && present;
        }

        public boolean isKnownAbsent() {
            return !occupancyUnknown && !present;
        }

        public boolean countUnknown() {
            return countUnknown;
        }

        /**
         * Reconciled integer count as a whole number, or {@link Double#NaN}
         * when unknown. A missing sensor is NaN, not zero.
         */
        public double count() {
            return count;
        }

        public MeasurementValidity occupancyValidity() {
            return occupancyValidity;
        }

        /**
         * 0 when unknown or disagreeing, 0.5 with a single usable source,
         * 1 when entry and count agree.
         */
        public double confidence() {
            return confidence;
        }

        /**
         * Reconciled TeamCode identity for {@code slot}, or empty when
         * unknown. Occupancy disagreement or a non-unique count leaves
         * identity unknown even if a label was configured.
         */
        public String identity(int slot) {
            String label = claimedIdentity(slot);
            return label == null ? "" : label;
        }

        public boolean identityUnknown(int slot) {
            return claimedIdentity(slot) == null;
        }

        public List<String> identities() {
            String[] copy = new String[capacity];
            for (int i = 0; i < capacity; i++) {
                String label = claimedIdentity(i);
                copy[i] = label == null ? "" : label;
            }
            return Collections.unmodifiableList(Arrays.asList(copy));
        }

        private String claimedIdentity(int slot) {
            if (slot < 0 || slot >= capacity) {
                throw new IllegalArgumentException("identity slot index out of range");
            }
            if (occupancyUnknown || countUnknown || !present) {
                return null;
            }
            int occupied = (int) Math.rint(count);
            boolean unique = occupied == capacity || (occupied == 1 && capacity == 1);
            if (!unique) {
                return null;
            }
            return labels[slot];
        }
    }
}
