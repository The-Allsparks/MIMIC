package org.allsparks.mimic.observe;

/** Validity classification for a single mechanism measurement. */
public enum MeasurementValidity {
    VALID,
    STALE,
    MISSING,
    OUT_OF_RANGE,
    UNSUPPORTED,
    DISAGREEING
}
