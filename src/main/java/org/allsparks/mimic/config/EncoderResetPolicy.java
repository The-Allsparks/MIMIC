package org.allsparks.mimic.config;

/**
 * When a later homing implementation may relabel encoder origin. This is not
 * a hardware RunMode and does not write motors.
 */
public enum EncoderResetPolicy {
    UNSPECIFIED,
    ON_COMPLETION,
    ON_REFERENCE,
    OFFSET_ONLY
}
