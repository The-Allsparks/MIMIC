package org.allsparks.mimic.control;

/**
 * Hardware edge for one actuator. MIMIC core does not import the FTC SDK.
 * The robot implements this with {@code setPower} / {@code setVelocity} /
 * {@code setTargetPosition} after {@link ActuatorSafetyGate} has allowed
 * the command.
 */
@FunctionalInterface
public interface ActuatorSink {
    /**
     * Apply one already-gated command. Implementations must not invent a
     * second power when {@code command} is stop.
     */
    void write(ActuatorCommand command);
}
