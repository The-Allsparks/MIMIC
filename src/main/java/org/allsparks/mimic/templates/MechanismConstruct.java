package org.allsparks.mimic.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard FTC layout under a {@link MechanismFamily}. TeamCode chooses which
 * constructs exist and names the hardware. Do not add season game-piece types
 * here.
 */
public enum MechanismConstruct {
    ROLLER_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Rollers or a swept roller that pull a piece off the field"),
    CLAW(
            MechanismFamily.INTAKE,
            MechanismMotionKind.POSITIONED,
            "Gripping jaws that close on a piece"),
    SPATULA(
            MechanismFamily.INTAKE,
            MechanismMotionKind.POSITIONED,
            "Blade or spatula that scoops a piece"),

    BELT(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Belt or conveyor along an internal path"),
    ROLLER_PATH(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Pinch or stacked rollers that move a piece through the robot"),
    INDEXER(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.DISCRETE,
            "Pockets, a star, or a drum that advance one piece at a time"),
    FEEDER(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Last transfer stage into a launcher"),

    FLYWHEEL(
            MechanismFamily.LAUNCHER,
            MechanismMotionKind.CONTINUOUS,
            "Spinning wheel that imparts velocity to a piece"),
    CATAPULT(
            MechanismFamily.LAUNCHER,
            MechanismMotionKind.POSITIONED,
            "Arm or pan that stores energy and releases it in a throw"),
    SPINAPULT(
            MechanismFamily.LAUNCHER,
            MechanismMotionKind.CONTINUOUS,
            "Spinning arm that both stores and releases launch energy"),
    TURRET(
            MechanismFamily.LAUNCHER,
            MechanismMotionKind.POSITIONED,
            "Yaw aiming axis, often composed with a launch-energy construct"),
    HOOD(
            MechanismFamily.LAUNCHER,
            MechanismMotionKind.POSITIONED,
            "Launch-angle aiming axis, often composed with a launch-energy construct"),

    ELEVATOR(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Vertical carriage, often cascaded stages"),
    LINEAR_SLIDE(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Telescoping or drawer-slide translation"),
    CAPSTAN(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Cable or string wrap that translates a carriage"),
    EXTENSION(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Horizontal or tray reach");

    private final MechanismFamily family;
    private final MechanismMotionKind motionKind;
    private final String summary;

    MechanismConstruct(MechanismFamily family, MechanismMotionKind motionKind, String summary) {
        this.family = family;
        this.motionKind = motionKind;
        this.summary = summary;
    }

    public MechanismFamily family() {
        return family;
    }

    public MechanismMotionKind motionKind() {
        return motionKind;
    }

    public String summary() {
        return summary;
    }

    /** True for flywheel, catapult, and spinapult. False for turret and hood. */
    public boolean isLaunchEnergy() {
        return this == FLYWHEEL || this == CATAPULT || this == SPINAPULT;
    }

    /** True for turret and hood aiming axes. */
    public boolean isLauncherAim() {
        return this == TURRET || this == HOOD;
    }

    public static List<MechanismConstruct> ofFamily(MechanismFamily family) {
        List<MechanismConstruct> matches = new ArrayList<>();
        for (MechanismConstruct construct : values()) {
            if (construct.family == family) {
                matches.add(construct);
            }
        }
        return Collections.unmodifiableList(matches);
    }
}
