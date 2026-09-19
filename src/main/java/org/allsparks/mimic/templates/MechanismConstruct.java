package org.allsparks.mimic.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard FTC layout under a {@link MechanismFamily}. TeamCode chooses which
 * constructs exist and names the hardware. Do not add season game-piece types
 * here.
 *
 * Custom layouts that are not in this enum use
 * {@code org.allsparks.mimic.config.ConstructDescriptor#custom}.
 */
public enum MechanismConstruct {
    ROLLER_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Rollers or a swept roller that pull a piece off the field"),
    COMPLIANT_WHEEL_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Compliant wheels that draw a piece into the robot"),
    VECTORED_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Angled rollers that pull a piece inward and rearward"),
    OVER_THE_TOP_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Intake that lifts a piece over a bumper or wall into the robot"),
    UNDER_BUMPER_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Low intake that acquires a piece under the bumper"),
    DEPLOYABLE_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.POSITIONED,
            "Intake that stows and deploys; compose with a roller construct"),
    SPATULA(
            MechanismFamily.INTAKE,
            MechanismMotionKind.POSITIONED,
            "Blade or spatula that scoops a piece"),
    VACUUM_INTAKE(
            MechanismFamily.INTAKE,
            MechanismMotionKind.CONTINUOUS,
            "Optional vacuum or blower intake; legality is team-verified each season"),

    BELT(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Belt or conveyor along an internal path"),
    ROLLER_PATH(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Stacked rollers that move a piece through the robot"),
    PINCH_ROLLER_PATH(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Pinch rollers that grip and move a piece along a path"),
    FEEDER(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Last transfer stage into a launcher"),
    COLOR_SORTER(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Transfer stage that routes by piece identity; compose with a diverter"),
    INDEXER(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.DISCRETE,
            "Pockets, a star, or a drum that advance one piece at a time"),
    ROTARY_MAGAZINE(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.DISCRETE,
            "Rotating magazine or carousel of pockets inside the robot"),
    HOPPER(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Bin or hopper that holds pieces in the internal path"),
    PIECE_ACCUMULATOR(
            MechanismFamily.TRANSFER,
            MechanismMotionKind.CONTINUOUS,
            "Accumulator that stacks or queues pieces before the next stage"),

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
    PUNCHER(
            MechanismFamily.LAUNCHER,
            MechanismMotionKind.POSITIONED,
            "Linear stored-energy striker that fires one shot per cycle"),

    ELEVATOR(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Vertical carriage; cascade vs continuous is conversion, not a second controller"),
    LINEAR_SLIDE(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Telescoping or drawer-slide translation"),
    EXTENSION(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Horizontal or tray reach"),
    CAPSTAN(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Cable or string wrap that translates a carriage"),
    RACK_AND_PINION(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Rack-and-pinion linear actuator"),
    LEAD_SCREW(
            MechanismFamily.LIFT,
            MechanismMotionKind.POSITIONED,
            "Lead-screw or ballscrew linear actuator"),

    PIVOT_ARM(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Single-pivot rotary arm"),
    SHOULDER(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Proximal rotary joint of an arm"),
    ELBOW(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Intermediate rotary joint of an arm"),
    WRIST(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Distal rotary joint before an end effector"),
    TURRET(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Yaw aiming or positioning axis, often composed with a launcher"),
    HOOD(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Launch-angle aiming axis, often composed with a launcher"),
    FOUR_BAR(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Four-bar linkage that keeps a pose while rotating"),
    VIRTUAL_FOUR_BAR(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Coordinated joints that behave like a four-bar"),
    LINKAGE_DEPLOYER(
            MechanismFamily.ARM,
            MechanismMotionKind.POSITIONED,
            "Linkage that deploys a mechanism through a rotary path"),

    CLAW(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Gripping jaws that close on a piece"),
    GATE(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Gate that opens or closes a path"),
    DIVERTER(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Diverter that chooses one of two paths"),
    BUCKET(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Bucket or dumper that tips to release pieces"),
    LATCH(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Latch that holds or releases a load or piece"),
    HOOK(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Hook that captures a bar, stone, or latch point"),
    PUSHER(
            MechanismFamily.END_EFFECTOR,
            MechanismMotionKind.POSITIONED,
            "Pusher that extends to contact a field or piece target"),

    WINCH(
            MechanismFamily.CLIMBER,
            MechanismMotionKind.POSITIONED,
            "Winch that winds a strap or cable for hang"),
    HOOK_DEPLOYER(
            MechanismFamily.CLIMBER,
            MechanismMotionKind.POSITIONED,
            "Deploys a hang hook; compose with a winch or latch"),
    RATCHET_ASSISTED_HANG(
            MechanismFamily.CLIMBER,
            MechanismMotionKind.POSITIONED,
            "Climb axis with a ratchet; ratchet state must be sensed"),

    CAROUSEL_SPINNER(
            MechanismFamily.FIELD_ELEMENT,
            MechanismMotionKind.CONTINUOUS,
            "Spinner that turns a field wheel or carousel"),
    FOUNDATION_GRABBER(
            MechanismFamily.FIELD_ELEMENT,
            MechanismMotionKind.POSITIONED,
            "Latch or hook that grabs a movable field foundation or goal"),
    BEACON_PUSHER(
            MechanismFamily.FIELD_ELEMENT,
            MechanismMotionKind.POSITIONED,
            "Pusher for a field beacon or similar button"),
    MARKER_DEPLOYER(
            MechanismFamily.FIELD_ELEMENT,
            MechanismMotionKind.POSITIONED,
            "One-shot deployer for a marker, cap, or aerial scoring element"),

    FUNNEL_GUIDE(
            MechanismFamily.PASSIVE,
            MechanismMotionKind.POSITIONED,
            "Passive funnel or guide; no assumed actuator"),
    DEPLOYABLE_STRUCTURE(
            MechanismFamily.PASSIVE,
            MechanismMotionKind.POSITIONED,
            "Passive or lightly deployed structure that is not a piece path");

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

    /** True for flywheel, catapult, spinapult, and puncher. False for turret and hood. */
    public boolean isLaunchEnergy() {
        return this == FLYWHEEL || this == CATAPULT || this == SPINAPULT || this == PUNCHER;
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
