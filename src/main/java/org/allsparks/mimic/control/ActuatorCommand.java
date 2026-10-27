package org.allsparks.mimic.control;

/**
 * One output a safety gate may hand to an {@link ActuatorSink}. Not a Hub
 * call. Effort is dimensionless. Velocity is ticks per second. Position is
 * an encoder target plus a power cap in {@code [0, 1]}.
 */
public final class ActuatorCommand {
    /** What the sink is allowed to do with this command. */
    public enum Kind {
        STOP,
        EFFORT,
        VELOCITY,
        POSITION
    }

    private final Kind kind;
    private final double effort;
    private final double velocity;
    private final int positionTicks;
    private final double power;

    private ActuatorCommand(Kind kind, double effort, double velocity, int positionTicks, double power) {
        this.kind = kind;
        this.effort = effort;
        this.velocity = velocity;
        this.positionTicks = positionTicks;
        this.power = power;
    }

    /** Open-loop zero. Brake or coast is the sink's zero-power behavior. */
    public static ActuatorCommand stop() {
        return new ActuatorCommand(Kind.STOP, 0.0, 0.0, 0, 0.0);
    }

    /** Dimensionless power. The gate clamps this to the configured effort limit. */
    public static ActuatorCommand effort(double effort) {
        return new ActuatorCommand(Kind.EFFORT, effort, 0.0, 0, 0.0);
    }

    /** Closed-loop speed in ticks per second. Sign is direction. */
    public static ActuatorCommand velocity(double ticksPerSecond) {
        return new ActuatorCommand(Kind.VELOCITY, 0.0, ticksPerSecond, 0, 0.0);
    }

    /**
     * Encoder target plus the power cap used while traveling there.
     * {@code power} is a magnitude, not a signed stick.
     */
    public static ActuatorCommand position(int targetTicks, double power) {
        return new ActuatorCommand(Kind.POSITION, 0.0, 0.0, targetTicks, power);
    }

    public Kind kind() {
        return kind;
    }

    public double effort() {
        return effort;
    }

    public double velocity() {
        return velocity;
    }

    public int positionTicks() {
        return positionTicks;
    }

    public double power() {
        return power;
    }
}
