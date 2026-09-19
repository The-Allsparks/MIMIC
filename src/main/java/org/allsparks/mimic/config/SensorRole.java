package org.allsparks.mimic.config;

/**
 * Job of a measurement. Channel names are team-owned. This is not an FTC
 * device class and does not read hardware.
 */
public enum SensorRole {
    RELATIVE_POSITION("ticks mapped to canonical pose", "canonical", true, false, true, false),
    ABSOLUTE_POSITION("absolute encoder or potentiometer", "canonical", true, false, true, false),
    VELOCITY("encoder velocity", "canonical/s", false, false, false, false),
    ACCELERATION("derived or IMU acceleration", "canonical/s^2", false, false, false, false),
    RETRACT_LIMIT("lower or retract limit switch", "asserted", true, true, false, false),
    EXTEND_LIMIT("upper or extend limit switch", "asserted", true, true, false, false),
    HOME_INDEX("home or index reference", "asserted", true, false, false, false),
    PIECE_ENTRY("piece present at entry", "asserted", false, false, false, true),
    PIECE_EXIT("piece present at exit", "asserted", false, false, false, true),
    PIECE_IDENTITY("piece classification", "identity", false, false, false, true),
    PIECE_COUNT("piece count evidence", "count", false, false, false, true),
    ACTUATOR_CURRENT("actuator current", "A", false, false, false, false),
    MOTOR_TEMPERATURE("motor temperature when a device reports it", "deg", false, false, false, false),
    LOAD_TENSION("load or cable tension", "load", false, false, false, false),
    LATCH_ENGAGED("latch confirmed engaged", "asserted", false, true, false, false),
    BRAKE_ENGAGED("brake confirmed engaged", "asserted", false, false, false, false),
    OBJECT_CONTACT("object contact or grip occupancy", "asserted", false, false, false, false),
    ACTUATOR_SIDE_POSITION("position at the actuator", "canonical", true, false, true, false),
    OUTPUT_SIDE_POSITION("position at the mechanism output", "canonical", true, false, true, false),
    REDUNDANT_POSITION("second position channel", "canonical", false, false, true, false),
    ORIENTATION("gravity or IMU orientation of the moving assembly", "rad", false, false, false, false),
    EXTERNAL_SERVO_FEEDBACK("measured servo or linkage pose, not command", "canonical", true, false, true, false);

    private final String typicalUse;
    private final String unitSymbol;
    private final boolean mayEstablishCalibration;
    private final boolean mayBeHardBoundary;
    private final boolean positionSource;
    private final boolean pieceEvidence;

    SensorRole(
            String typicalUse,
            String unitSymbol,
            boolean mayEstablishCalibration,
            boolean mayBeHardBoundary,
            boolean positionSource,
            boolean pieceEvidence) {
        this.typicalUse = typicalUse;
        this.unitSymbol = unitSymbol;
        this.mayEstablishCalibration = mayEstablishCalibration;
        this.mayBeHardBoundary = mayBeHardBoundary;
        this.positionSource = positionSource;
        this.pieceEvidence = pieceEvidence;
    }

    public String typicalUse() {
        return typicalUse;
    }

    public String unitSymbol() {
        return unitSymbol;
    }

    public boolean mayEstablishCalibration() {
        return mayEstablishCalibration;
    }

    public boolean mayBeHardBoundary() {
        return mayBeHardBoundary;
    }

    public boolean isPositionSource() {
        return positionSource;
    }

    public boolean isHomingReference() {
        return this == HOME_INDEX
                || this == RETRACT_LIMIT
                || this == EXTEND_LIMIT
                || this == ABSOLUTE_POSITION
                || this == EXTERNAL_SERVO_FEEDBACK;
    }

    public boolean isVelocitySource() {
        return this == VELOCITY;
    }

    public boolean isPieceEvidence() {
        return pieceEvidence;
    }

    public boolean isPiecePassage() {
        return this == PIECE_ENTRY || this == PIECE_EXIT || this == PIECE_COUNT;
    }
}
