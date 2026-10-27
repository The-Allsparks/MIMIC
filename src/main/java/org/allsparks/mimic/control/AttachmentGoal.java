package org.allsparks.mimic.control;

/**
 * One loop's request for an attachment. The number's meaning depends on
 * {@link #kind()}. This does not write hardware.
 */
public final class AttachmentGoal {
    /** Which command the law should build. */
    public enum Kind {
        EFFORT,
        HOLD_VELOCITY,
        REVERSE,
        STOP,
        TRIGGER
    }

    private final Kind kind;
    private final double value;

    private AttachmentGoal(Kind kind, double value) {
        this.kind = kind;
        this.value = value;
    }

    /** Signed open-loop power. Positive and negative are both motion. */
    public static AttachmentGoal effort(double power) {
        return new AttachmentGoal(Kind.EFFORT, power);
    }

    /** Hold this ticks-per-second. The law does not choose the RPM. */
    public static AttachmentGoal holdVelocity(double ticksPerSecond) {
        return new AttachmentGoal(Kind.HOLD_VELOCITY, ticksPerSecond);
    }

    /** Open-loop reverse. The magnitude is the caller's reverse power. */
    public static AttachmentGoal reverse(double power) {
        return new AttachmentGoal(Kind.REVERSE, power);
    }

    /** Park. Laws must not keep a previous motion running. */
    public static AttachmentGoal stop() {
        return new AttachmentGoal(Kind.STOP, 0.0);
    }

    /**
     * Analog trigger travel in {@code 0..1}. The index law decides whether
     * this sample is a full pull.
     */
    public static AttachmentGoal trigger(double travel) {
        return new AttachmentGoal(Kind.TRIGGER, travel);
    }

    public Kind kind() {
        return kind;
    }

    /** Effort, ticks per second, reverse power, or trigger travel. */
    public double value() {
        return value;
    }

    /** True when a repeat of this goal would not be a new request. */
    public boolean sameRequest(AttachmentGoal other) {
        if (other == null || other.kind != kind) {
            return false;
        }
        return Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
    }
}
