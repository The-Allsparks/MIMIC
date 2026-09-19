package org.allsparks.mimic.config;

import org.allsparks.mimic.api.GoalDisposition;

/**
 * Explicit fail action for an {@link InterlockRule}. Table evaluation maps
 * each value onto an existing {@link GoalDisposition}. This is not a
 * scheduler, not a Command queue, and does not write motors or servos.
 *
 * CONFIRM does not add a {@code GoalDisposition} constant. It maps to
 * {@link GoalDisposition#DEFERRED} with reason
 * {@link InterlockRule#CONFIRM_REQUIRED}: wait for an explicit named
 * confirmation, do not invent a robot-wide prompt loop.
 *
 * INTERMEDIATE maps to {@link GoalDisposition#REPLACED}. Generated
 * intermediates must not cycle; see {@link InterlockRule}.
 */
public enum InterlockOutcome {
    REJECT,
    DEFER,
    CLAMP,
    INTERMEDIATE,
    CONFIRM;

    /**
     * Disposition used when this fail action fires. Fail actions never
     * accept the original goal.
     */
    public GoalDisposition disposition() {
        switch (this) {
            case REJECT:
                return GoalDisposition.REJECTED;
            case DEFER:
                return GoalDisposition.DEFERRED;
            case CLAMP:
                return GoalDisposition.CLAMPED;
            case INTERMEDIATE:
                return GoalDisposition.REPLACED;
            case CONFIRM:
                return GoalDisposition.DEFERRED;
            default:
                throw new IllegalStateException("unmapped outcome " + name());
        }
    }
}
