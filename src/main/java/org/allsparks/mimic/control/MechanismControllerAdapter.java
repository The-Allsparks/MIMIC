package org.allsparks.mimic.control;

import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Replaceable controller / feedforward seam: setpoint in, dimensionless
 * effort out, no hardware.
 *
 * Core MIMIC is MIT and does not ship a PID, WPILib-style feedforward, or a
 * NextControl (GPL-3.0) compile dependency. Real adapters belong in TeamCode
 * or tests; teams who wrap NextControl do that on the robot side with a
 * license warning. {@code MimicSession} must not call this interface.
 * {@code ControlDomain} stays a configuration declaration; declaring
 * {@code PROFILED_POSITION} does not run control.
 *
 * Returned effort is a number in {@code [-1, 1]}, matching snapshot
 * requested/applied output. It is not {@code setPower} and must not reach
 * motors until an actuator safety gate exists.
 */
@FunctionalInterface
public interface MechanismControllerAdapter {

    /**
     * Compute effort from the current snapshot and setpoint. Implementations
     * must not write motors or servos.
     *
     * @param snap current observation; must not be {@code null}
     * @param setpoint instantaneous reference; must not be {@code null}
     * @return dimensionless effort in {@code [-1, 1]}
     */
    double effort(MechanismSnapshot snap, Setpoint setpoint);
}
