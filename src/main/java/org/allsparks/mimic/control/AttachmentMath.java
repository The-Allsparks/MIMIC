package org.allsparks.mimic.control;

/**
 * Stick, trigger, and encoder math shared by attachment laws. No Hub and
 * no gamepad. Callers pass the robot's ticks-per-revolution and RPM.
 */
public final class AttachmentMath {
    /** Travel at or above this is a full pull. */
    public static final double TRIGGER_FULL = 0.90;
    /** Travel at or below this rearms the next full pull. */
    public static final double TRIGGER_REARM = 0.20;

    private AttachmentMath() {}

    /** One trigger decision. {@code index} is true for a single sample. */
    public static final class Pull {
        public final boolean index;
        public final boolean armed;

        private Pull(boolean index, boolean armed) {
            this.index = index;
            this.armed = armed;
        }
    }

    /** NaN and out-of-range stick values become a legal effort in {@code [-1, 1]}. */
    public static double clampEffort(double power) {
        if (!Double.isFinite(power)) {
            return 0.0;
        }
        if (power > 1.0) {
            return 1.0;
        }
        if (power < -1.0) {
            return -1.0;
        }
        return power;
    }

    /**
     * True when a hold is active and one measured speed is inside the band.
     * NaN or a negative band is not in band.
     */
    public static boolean inBand(double measuredTicksPerSecond, double targetTicksPerSecond, double bandTicksPerSecond) {
        if (!Double.isFinite(measuredTicksPerSecond)
                || !Double.isFinite(targetTicksPerSecond)
                || !Double.isFinite(bandTicksPerSecond)
                || bandTicksPerSecond < 0.0) {
            return false;
        }
        return Math.abs(measuredTicksPerSecond - targetTicksPerSecond) <= bandTicksPerSecond;
    }

    /**
     * True only while holding and both speeds are inside the band. Reverse
     * and coast are not at speed.
     */
    public static boolean atSpeed(
            boolean holding,
            double leftTicksPerSecond,
            double rightTicksPerSecond,
            double targetTicksPerSecond,
            double bandTicksPerSecond) {
        if (!holding) {
            return false;
        }
        return inBand(leftTicksPerSecond, targetTicksPerSecond, bandTicksPerSecond)
                && inBand(rightTicksPerSecond, targetTicksPerSecond, bandTicksPerSecond);
    }

    /**
     * Rising edge of a full pull. A full pull while {@code atSpeed} is false
     * spends the edge and does not index. Holding the trigger does not repeat.
     */
    public static Pull pull(boolean armed, double trigger, boolean atSpeed) {
        double travel = Double.isFinite(trigger) ? trigger : 0.0;
        boolean full = travel >= TRIGGER_FULL;
        boolean index = armed && full && atSpeed;
        boolean nextArmed;
        if (travel <= TRIGGER_REARM) {
            nextArmed = true;
        } else if (full) {
            nextArmed = false;
        } else {
            nextArmed = armed;
        }
        return new Pull(index, nextArmed);
    }

    /**
     * Encoder ticks for {@code indexes} completed third-turns. Rounding the
     * total keeps three steps equal to one revolution, plus at most one count.
     */
    public static int indexTicks(int indexes, double ticksPerRevolution) {
        if (indexes <= 0 || !Double.isFinite(ticksPerRevolution) || ticksPerRevolution <= 0.0) {
            return 0;
        }
        return (int) Math.round(indexes * (ticksPerRevolution / 3.0));
    }

    /** Arrival band: about 4% of one index, and at least 3 ticks. */
    public static int settleTicks(double ticksPerRevolution) {
        double third = Double.isFinite(ticksPerRevolution) ? ticksPerRevolution / 3.0 : 0.0;
        int band = (int) Math.round(Math.abs(third) * 0.04);
        if (band < 3) {
            return 3;
        }
        return band;
    }

    /** True when {@code ticks} is within {@code band} of {@code target}. NaN is not arrived. */
    public static boolean nearTarget(double ticks, int target, int band) {
        if (!Double.isFinite(ticks)) {
            return false;
        }
        return Math.abs(ticks - target) <= band;
    }

    /** Hub ticks per second for an output-shaft RPM. Non-positive ratio returns 0. */
    public static double ticksPerSecond(double ticksPerRevolution, double rpm) {
        if (!Double.isFinite(ticksPerRevolution) || !Double.isFinite(rpm) || ticksPerRevolution <= 0.0) {
            return 0.0;
        }
        return ticksPerRevolution * rpm / 60.0;
    }

    /**
     * REV Hub velocity feedforward. 32767 is full scale inside the hub.
     * Returns 0 when free speed is unknown so the caller does not invent a gain.
     */
    public static double velocityFeedforward(double ticksPerRevolution, double freeRpm) {
        double maxTicksPerSecond = ticksPerSecond(ticksPerRevolution, freeRpm);
        if (maxTicksPerSecond <= 0.0) {
            return 0.0;
        }
        return 32767.0 / maxTicksPerSecond;
    }
}
