package org.allsparks.mimic.config;

/**
 * Documented fault levels from fault-handling.md. Declaration order is
 * increasing severity. This enum does not command hardware.
 *
 * {@link #STOP_DEPENDENCIES} and {@link #STOP_ROBOT} exist so the table
 * can name them. Catalog defaults in {@link FaultKind} do not request a
 * full robot stop.
 */
public enum FaultSeverity {
    INFO(0),
    DEGRADED(1),
    STOP_MECHANISM(2),
    STOP_DEPENDENCIES(3),
    STOP_ROBOT(4);

    private final int rank;

    FaultSeverity(int rank) {
        this.rank = rank;
    }

    /**
     * Integer rank for table combination. Higher means more severe.
     */
    public int rank() {
        return rank;
    }

    /**
     * The more severe of two levels. Used when a kind's default floors a
     * milder {@link DegradedBehavior} mapping.
     */
    public static FaultSeverity stricter(FaultSeverity left, FaultSeverity right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.rank >= right.rank ? left : right;
    }

    /**
     * Map a per-role degraded declaration onto a severity. Optional ignore
     * is log-only; disable-capability is reduced DOF (DEGRADED), not a stop.
     */
    public static FaultSeverity fromDegradedBehavior(DegradedBehavior behavior) {
        if (behavior == null) {
            throw new NullPointerException("behavior");
        }
        switch (behavior) {
            case IGNORE_OPTIONAL:
                return INFO;
            case MARK_DEGRADED:
                return DEGRADED;
            case DISABLE_CAPABILITY:
                return DEGRADED;
            case STOP_MECHANISM:
                return STOP_MECHANISM;
            default:
                throw new IllegalStateException("unmapped behavior " + behavior);
        }
    }
}
