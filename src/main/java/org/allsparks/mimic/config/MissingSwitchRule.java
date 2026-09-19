package org.allsparks.mimic.config;

import org.allsparks.mimic.observe.LimitSwitchSample;
import org.allsparks.mimic.observe.MeasurementValidity;

/**
 * How a {@link LimitSwitchSample} that is not a clear reading may be used.
 *
 * A missing switch is not "not asserted." {@link LimitSwitchSample#missing()}
 * stores {@code asserted = false} because a Java boolean needs a value. That
 * {@code false} is not a measurement of "clear of the limit." Travel into
 * that limit must not be authorized from a {@link MeasurementValidity#MISSING}
 * sample.
 *
 * This enum has one value on purpose. A "treat missing as clear" option would
 * hide a disconnected switch as a green light.
 */
public enum MissingSwitchRule {
    NEVER_TREAT_AS_CLEAR
}
