package org.allsparks.mimic.config;

/**
 * Declared control domain. Not a running controller. Passive observation is
 * the only domain Phase 0 actually performs.
 */
public enum ControlDomain {
    OPEN_LOOP_EFFORT,
    VOLTAGE_COMPENSATED_EFFORT,
    VELOCITY,
    POSITION,
    PROFILED_POSITION,
    DISCRETE_INDEX,
    NAMED_STATE,
    STORED_ENERGY_CYCLE,
    PASSIVE_OBSERVATION
}
