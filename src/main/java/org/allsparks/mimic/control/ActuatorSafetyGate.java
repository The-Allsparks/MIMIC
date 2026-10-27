package org.allsparks.mimic.control;

import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Last check before an {@link ActuatorSink} write. A law can ask for an
 * unsafe number; this replaces it with {@link ActuatorCommand#stop()} or a
 * clamped command. It does not call Hub methods.
 *
 * <p>Open-loop effort does not require a sensor. Velocity and position
 * commands require a usable sample of that quantity. A missing, stale, or
 * non-finite sample stops that actuator. Phase flags do not turn this on.
 * A session writes only after {@code controlWith} installs a gate and a sink.
 */
public final class ActuatorSafetyGate {
    public static final String PASSED = "PASSED";
    public static final String CLAMPED = "CLAMPED";
    public static final String STOPPED = "STOPPED_UNSAFE";

    private final double maxAbsEffort;
    private final double maxAbsVelocity;

    private ActuatorSafetyGate(double maxAbsEffort, double maxAbsVelocity) {
        this.maxAbsEffort = maxAbsEffort;
        this.maxAbsVelocity = maxAbsVelocity;
    }

    /**
     * Effort and position only. A velocity request is stopped. Use this for
     * a stick intake or a position index.
     */
    public static ActuatorSafetyGate effortAndPosition(double maxAbsEffort) {
        requirePositive("maxAbsEffort", maxAbsEffort);
        return new ActuatorSafetyGate(maxAbsEffort, 0.0);
    }

    /**
     * Effort and velocity. {@code maxAbsVelocity} is the largest ticks-per-second
     * magnitude the sink may be given, typically the motor's free speed.
     */
    public static ActuatorSafetyGate effortAndVelocity(double maxAbsEffort, double maxAbsVelocity) {
        requirePositive("maxAbsEffort", maxAbsEffort);
        requirePositive("maxAbsVelocity", maxAbsVelocity);
        return new ActuatorSafetyGate(maxAbsEffort, maxAbsVelocity);
    }

    /**
     * Command the sink is allowed to write. Null in becomes stop. This method
     * does not write hardware.
     */
    public GateDecision allow(MechanismSnapshot snapshot, ActuatorCommand requested) {
        if (requested == null || requested.kind() == ActuatorCommand.Kind.STOP) {
            return pass(ActuatorCommand.stop());
        }
        switch (requested.kind()) {
            case EFFORT:
                return allowEffort(requested.effort());
            case VELOCITY:
                return allowVelocity(snapshot, requested.velocity());
            case POSITION:
                return allowPosition(snapshot, requested);
            default:
                return stopped();
        }
    }

    private GateDecision allowEffort(double effort) {
        if (!Double.isFinite(effort)) {
            return stopped();
        }
        double clamped = clamp(effort, -maxAbsEffort, maxAbsEffort);
        if (clamped != effort) {
            return new GateDecision(ActuatorCommand.effort(clamped), CLAMPED);
        }
        return pass(ActuatorCommand.effort(effort));
    }

    private GateDecision allowVelocity(MechanismSnapshot snapshot, double ticksPerSecond) {
        if (maxAbsVelocity <= 0.0) {
            return stopped();
        }
        if (snapshot == null || !snapshot.velocitySample().isUsable()) {
            return stopped();
        }
        if (!Double.isFinite(ticksPerSecond)) {
            return stopped();
        }
        double clamped = clamp(ticksPerSecond, -maxAbsVelocity, maxAbsVelocity);
        if (clamped != ticksPerSecond) {
            return new GateDecision(ActuatorCommand.velocity(clamped), CLAMPED);
        }
        return pass(ActuatorCommand.velocity(ticksPerSecond));
    }

    private GateDecision allowPosition(MechanismSnapshot snapshot, ActuatorCommand requested) {
        if (snapshot == null || !snapshot.positionSample().isUsable()) {
            return stopped();
        }
        if (!Double.isFinite(requested.power())) {
            return stopped();
        }
        double power = clamp(requested.power(), 0.0, maxAbsEffort);
        if (power != requested.power()) {
            return new GateDecision(ActuatorCommand.position(requested.positionTicks(), power), CLAMPED);
        }
        return pass(requested);
    }

    private static GateDecision pass(ActuatorCommand command) {
        return new GateDecision(command, PASSED);
    }

    private static GateDecision stopped() {
        return new GateDecision(ActuatorCommand.stop(), STOPPED);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void requirePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
    }
}
