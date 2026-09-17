package org.allsparks.mimic.observe;

import java.util.Objects;
import org.allsparks.mimic.config.SensorRole;

/**
 * Observe-only piece presence and count from snapshot role extras.
 *
 * Reads {@link SensorRole#PIECE_ENTRY}, {@link SensorRole#PIECE_EXIT}, and
 * {@link SensorRole#PIECE_COUNT} through {@link MechanismSnapshot#role}.
 * Missing or unwired sensors are unknown occupancy, not empty and not a
 * count of zero. Channel observation only: identity, capacity, and
 * reconcile live on optional {@link PieceTracker}. Never writes hardware.
 */
public final class PieceObservation {
    private final Presence entry;
    private final Presence exit;
    private final Count count;

    private PieceObservation(Presence entry, Presence exit, Count count) {
        this.entry = Objects.requireNonNull(entry, "entry");
        this.exit = Objects.requireNonNull(exit, "exit");
        this.count = Objects.requireNonNull(count, "count");
    }

    public static PieceObservation from(MechanismSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new PieceObservation(
                Presence.fromDigital(snapshot.role(SensorRole.PIECE_ENTRY)),
                Presence.fromDigital(snapshot.role(SensorRole.PIECE_EXIT)),
                Count.fromNumeric(snapshot.role(SensorRole.PIECE_COUNT)));
    }

    public Presence entry() {
        return entry;
    }

    public Presence exit() {
        return exit;
    }

    public Count count() {
        return count;
    }

    /**
     * True when no entry, exit, or count channel is usable. A missing
     * sensor does not mean the path is empty.
     */
    public boolean occupancyUnknown() {
        return entry.isUnknown() && exit.isUnknown() && count.isUnknown();
    }

    /**
     * Presence at one passage ({@link SensorRole#PIECE_ENTRY} or
     * {@link SensorRole#PIECE_EXIT}). {@link #present()} is only occupancy
     * when {@link #validity()} is {@link MeasurementValidity#VALID}.
     */
    public static final class Presence {
        private final boolean present;
        private final MeasurementValidity validity;
        private final String channelId;
        private final long capturedAtNanos;

        private Presence(
                boolean present,
                MeasurementValidity validity,
                String channelId,
                long capturedAtNanos) {
            this.present = present;
            this.validity = Objects.requireNonNull(validity, "validity");
            this.channelId = channelId == null ? "" : channelId;
            this.capturedAtNanos = capturedAtNanos;
        }

        static Presence fromDigital(RoleSample sample) {
            Objects.requireNonNull(sample, "sample");
            LimitSwitchSample digital = sample.digital();
            if (!sample.hasDigital()) {
                return new Presence(
                        false,
                        MeasurementValidity.UNSUPPORTED,
                        digital.channelId(),
                        digital.capturedAtNanos());
            }
            return new Presence(
                    digital.asserted(),
                    digital.validity(),
                    digital.channelId(),
                    digital.capturedAtNanos());
        }

        /**
         * Interpreted present state. Meaningful only when
         * {@link #validity()} is {@link MeasurementValidity#VALID}. False
         * here is not empty occupancy when the channel is unknown.
         */
        public boolean present() {
            return present;
        }

        public MeasurementValidity validity() {
            return validity;
        }

        public String channelId() {
            return channelId;
        }

        public long capturedAtNanos() {
            return capturedAtNanos;
        }

        /**
         * True when this passage cannot be claimed occupied or empty.
         * {@link MeasurementValidity#MISSING} and
         * {@link MeasurementValidity#UNSUPPORTED} are unknown, not absent.
         */
        public boolean isUnknown() {
            return validity != MeasurementValidity.VALID;
        }

        public boolean isKnownPresent() {
            return validity == MeasurementValidity.VALID && present;
        }

        public boolean isKnownAbsent() {
            return validity == MeasurementValidity.VALID && !present;
        }
    }

    /**
     * Count evidence from {@link SensorRole#PIECE_COUNT}. A missing or
     * unwired sensor is unknown, not zero.
     */
    public static final class Count {
        private final SensorSample sample;

        private Count(SensorSample sample) {
            this.sample = Objects.requireNonNull(sample, "sample");
        }

        static Count fromNumeric(RoleSample sample) {
            Objects.requireNonNull(sample, "sample");
            SensorSample numeric = sample.numeric();
            if (!sample.hasNumeric()) {
                String unit = numeric.unitSymbol().isEmpty()
                        ? SensorRole.PIECE_COUNT.unitSymbol()
                        : numeric.unitSymbol();
                return new Count(SensorSample.unsupported(
                        numeric.capturedAtNanos(), numeric.channelId(), unit));
            }
            return new Count(numeric);
        }

        public double value() {
            return sample.value();
        }

        public MeasurementValidity validity() {
            return sample.validity();
        }

        public String channelId() {
            return sample.channelId();
        }

        public String unitSymbol() {
            return sample.unitSymbol();
        }

        public long capturedAtNanos() {
            return sample.capturedAtNanos();
        }

        /**
         * True when the count cannot be claimed. {@code MISSING} and
         * {@code UNSUPPORTED} are unknown occupancy, not a count of zero.
         * A {@code VALID} value of {@code 0} is known empty.
         */
        public boolean isUnknown() {
            return !sample.isUsable();
        }

        public boolean isKnown() {
            return sample.isUsable();
        }
    }
}
