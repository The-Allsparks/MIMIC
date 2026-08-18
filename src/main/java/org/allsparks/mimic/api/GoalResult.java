package org.allsparks.mimic.api;

import java.util.Objects;

/**
 * Result of {@link MimicMechanism#requestGoal(Object)}.
 *
 * Phase 0 always returns a rejection with reason {@code NO_ACTIVE_CONTROL}.
 */
public final class GoalResult {
    private final boolean accepted;
    private final GoalDisposition disposition;
    private final String reason;

    public GoalResult(boolean accepted, GoalDisposition disposition, String reason) {
        if (disposition == null) {
            throw new IllegalArgumentException("disposition is required");
        }
        if (reason == null || reason.isEmpty()) {
            throw new IllegalArgumentException("reason must be non-empty");
        }
        this.accepted = accepted;
        this.disposition = disposition;
        this.reason = reason;
    }

    public static GoalResult rejected(String reason) {
        return new GoalResult(false, GoalDisposition.REJECTED, reason);
    }

    public static GoalResult accepted(String reason) {
        return new GoalResult(true, GoalDisposition.ACCEPTED, reason);
    }

    public boolean accepted() {
        return accepted;
    }

    public GoalDisposition disposition() {
        return disposition;
    }

    public String reason() {
        return reason;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GoalResult)) {
            return false;
        }
        GoalResult that = (GoalResult) other;
        return accepted == that.accepted
                && disposition == that.disposition
                && reason.equals(that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accepted, disposition, reason);
    }
}
