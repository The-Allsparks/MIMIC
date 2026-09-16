package org.allsparks.mimic.templates;

/**
 * How a construct typically moves. Observation still uses
 * {@code MechanismUnits}; this is a catalog hint, not a controller.
 */
public enum MechanismMotionKind {
    /** Spins or rolls while a piece is in contact (intake roller, flywheel). */
    CONTINUOUS,
    /** Goes to a pose or cocking angle (elevator, turret, hood, catapult). */
    POSITIONED,
    /** Advances one pocket or slot at a time (indexer). */
    DISCRETE
}
