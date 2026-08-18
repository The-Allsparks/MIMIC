package org.allsparks.mimic;

/**
 * Development phases for MIMIC. Higher phases build on lower phases and are
 * disabled by default until acceptance criteria are met.
 *
 * Phases 2+ may later command hardware when explicitly enabled. Phase 0 and 1
 * must never command motors or servos.
 */
public enum MimicPhase {
    /** Hardware-independent contracts, snapshots, and measurement validation. */
    PHASE_0_CONTRACTS,
    /** Passive mechanism observation and telemetry. No actuation. */
    PHASE_1_OBSERVATION,
    /** Calibration and homing lifecycle. */
    PHASE_2_CALIBRATION,
    /** Limits, safe manual control, and the actuator safety gate. */
    PHASE_3_LIMITS,
    /** Profiled motion with replaceable controllers. */
    PHASE_4_PROFILED_CONTROL,
    /** Multi-actuator synchronization. */
    PHASE_5_SYNCHRONIZATION,
    /** Semantic mechanism states. */
    PHASE_6_STATES,
    /** Cross-mechanism interlocks. */
    PHASE_7_INTERLOCKS,
    /** Fault detection and bounded recovery. */
    PHASE_8_FAULTS,
    /** AMPER power-request integration. */
    PHASE_9_AMPER,
    /** Characterization and simulation. */
    PHASE_10_SIMULATION
}
