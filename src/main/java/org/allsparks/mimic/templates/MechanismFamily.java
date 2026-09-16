package org.allsparks.mimic.templates;

/**
 * Season-agnostic job a mechanism performs. Constructs under a family are
 * standard FTC layouts, not this year's robot.
 *
 * A blueprint is metadata. It does not command motors or servos.
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
     * Send a piece off the robot. Includes launch-energy layouts (flywheel,
     * catapult, spinapult) and aiming axes often composed with them (turret,
     * hood).
     */
    LAUNCHER("Send a piece off the robot, including launch energy and aiming"),
    /**
     * Translate a carriage, stage, or tray. Elevator is the common vertical
     * construct; slides and extensions belong here too.
     */
    LIFT("Translate a carriage, stage, or tray");

    private final String purpose;

    MechanismFamily(String purpose) {
        this.purpose = purpose;
    }

    public String purpose() {
        return purpose;
    }
}
