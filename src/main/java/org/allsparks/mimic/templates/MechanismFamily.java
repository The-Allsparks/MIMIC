package org.allsparks.mimic.templates;

/**
 * Season-agnostic job a mechanism performs. Constructs under a family are
 * standard FTC layouts, not this year's robot.
 *
 * A blueprint or configuration is metadata. It does not command motors or
 * servos. New families require a library change; custom constructs pick one
 * of these jobs.
 */
public enum MechanismFamily {
    /** Acquire a game piece from the field. */
    INTAKE("Acquire a game piece from the field"),
    /**
     * Move a piece along a path inside the robot. Prefer this word over
     * "shoot" for internal conveyance. Launching off the robot is
     * {@link #LAUNCHER}.
     */
    TRANSFER("Move a piece along a path inside the robot"),
    /**
     * Impart energy so a piece leaves the robot. Aim axes (turret, hood)
     * belong to {@link #ARM} and are composed with a launcher.
     */
    LAUNCHER("Impart energy so a piece leaves the robot"),
    /**
     * Translate a carriage, stage, or tray. Elevator is the common vertical
     * construct; slides and extensions belong here too.
     */
    LIFT("Translate a carriage, stage, or tray"),
    /** Rotary positioning: pivot, wrist, turret, hood, four-bar. */
    ARM("Rotate a joint or aiming axis"),
    /** Named-state tool: claw, gate, bucket, latch, hook, pusher. */
    END_EFFECTOR("Named-state tool at the end of a kinematic chain"),
    /** Hang the robot from a field structure while loaded. */
    CLIMBER("Support the robot from a hang structure while loaded"),
    /** Actuate a field scoring device the robot does not own. */
    FIELD_ELEMENT("Actuate a field scoring device"),
    /** Funnel, guide, or structure with no piece-path job of its own. */
    PASSIVE("Passive or deployable structure");

    private final String purpose;

    MechanismFamily(String purpose) {
        this.purpose = purpose;
    }

    public String purpose() {
        return purpose;
    }
}
