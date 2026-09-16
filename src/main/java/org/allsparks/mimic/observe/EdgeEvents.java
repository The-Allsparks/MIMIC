package org.allsparks.mimic.observe;

/**
 * Rising and falling edges between two digital samples.
 *
 * <p>{@code null} (missing) is not treated as not-asserted, so it cannot
 * create an edge. Unusable {@link LimitSwitchSample}s are the same: do not
 * read {@link LimitSwitchSample#asserted()} when the sample is missing; that
 * field is {@code false} and would look like a release.
 */
public final class EdgeEvents {
    private EdgeEvents() {
    }

    public static boolean rising(boolean previousAsserted, boolean currentAsserted) {
        return !previousAsserted && currentAsserted;
    }

    public static boolean falling(boolean previousAsserted, boolean currentAsserted) {
        return previousAsserted && !currentAsserted;
    }

    /**
     * @param previousAsserted prior usable value, or {@code null} if missing
     * @param currentAsserted current usable value, or {@code null} if missing
     */
    public static boolean rising(Boolean previousAsserted, Boolean currentAsserted) {
        return Boolean.FALSE.equals(previousAsserted) && Boolean.TRUE.equals(currentAsserted);
    }

    /**
     * @param previousAsserted prior usable value, or {@code null} if missing
     * @param currentAsserted current usable value, or {@code null} if missing
     */
    public static boolean falling(Boolean previousAsserted, Boolean currentAsserted) {
        return Boolean.TRUE.equals(previousAsserted) && Boolean.FALSE.equals(currentAsserted);
    }

    public static boolean rising(LimitSwitchSample previous, LimitSwitchSample current) {
        return rising(usableAsserted(previous), usableAsserted(current));
    }

    public static boolean falling(LimitSwitchSample previous, LimitSwitchSample current) {
        return falling(usableAsserted(previous), usableAsserted(current));
    }

    private static Boolean usableAsserted(LimitSwitchSample sample) {
        if (sample == null || !sample.isUsable()) {
            return null;
        }
        return Boolean.valueOf(sample.asserted());
    }
}
