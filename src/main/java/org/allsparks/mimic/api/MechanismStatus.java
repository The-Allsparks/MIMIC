package org.allsparks.mimic.api;

/**
 * High-level mechanism health for telemetry. Not a command scheduler state.
 */
public enum MechanismStatus {
    OBSERVING,
    UNCALIBRATED,
    READY,
    MOVING,
    HOLDING,
    DEGRADED,
    FAULTED,
    STOPPED
}
