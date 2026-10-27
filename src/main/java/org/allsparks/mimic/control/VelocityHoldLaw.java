package org.allsparks.mimic.control;

import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * One flywheel. Hold is a velocity command. Reverse is open-loop effort and
 * wins over hold when the caller sends {@link AttachmentGoal#reverse(double)}.
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
            return ActuatorCommand.effort(goal.value());
        }
        if (goal != null && goal.kind() == AttachmentGoal.Kind.HOLD_VELOCITY) {
            double measured = usableVelocity(snapshot);
            atSpeed = AttachmentMath.inBand(measured, goal.value(), bandTicksPerSecond);
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
