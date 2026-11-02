package org.allsparks.mimic.control;

import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * One flywheel. Hold is a velocity command. Reverse is a velocity command too,
 * at the caller's ticks per second, and it wins over hold. It does not count
 * as at-speed, so a slow reverse cannot arm a shot.
 * {@link #atSpeed()} is this wheel only.
 */
public final class VelocityHoldLaw implements AttachmentLaw {
    private final double bandTicksPerSecond;
    private boolean atSpeed;

    public VelocityHoldLaw(double bandTicksPerSecond) {
        if (!Double.isFinite(bandTicksPerSecond) || bandTicksPerSecond < 0.0) {
            throw new IllegalArgumentException("bandTicksPerSecond must be >= 0");
        }
        this.bandTicksPerSecond = bandTicksPerSecond;
    }

    /** True after the latest command while this wheel's measured speed is inside the band. */
    public boolean atSpeed() {
        return atSpeed;
    }

    @Override
    public ActuatorCommand command(MechanismSnapshot snapshot, AttachmentGoal goal, AttachmentContext context) {
        if (goal != null && goal.kind() == AttachmentGoal.Kind.REVERSE) {
            atSpeed = false;
            return ActuatorCommand.velocity(goal.value());
        }
        if (goal != null && goal.kind() == AttachmentGoal.Kind.HOLD_VELOCITY) {
            double measured = usableVelocity(snapshot);
            // Magnitude. A mirrored flywheel reads the opposite sign of the target and would
            // otherwise stay outside the band, so a full trigger pull never indexes.
            atSpeed = AttachmentMath.inBand(
                    Math.abs(measured), Math.abs(goal.value()), bandTicksPerSecond);
            return ActuatorCommand.velocity(goal.value());
        }
        atSpeed = false;
        return ActuatorCommand.stop();
    }

    @Override
    public void onStop() {
        atSpeed = false;
    }

    private static double usableVelocity(MechanismSnapshot snapshot) {
        if (snapshot == null || !snapshot.velocitySample().isUsable()) {
            return Double.NaN;
        }
        return snapshot.velocity();
    }
}
