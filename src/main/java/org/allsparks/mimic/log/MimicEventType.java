package org.allsparks.mimic.log;

/** Categories of logged mechanism events. Compatible with a future TRACE sink. */
public enum MimicEventType {
    LOOP_SAMPLE,
    GOAL_REQUESTED,
    GOAL_REJECTED,
    GOAL_ACCEPTED,
    CALIBRATION_TRANSITION,
    SENSOR_INVALID,
    LIMIT_ASSERTED,
    FAULT,
    RECOVERY,
    AMPER_CONSTRAINT,
    SAFETY_GATE,
    STOP_REQUESTED,
    MATCH_SUMMARY
}
