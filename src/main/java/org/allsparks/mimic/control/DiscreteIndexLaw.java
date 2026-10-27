package org.allsparks.mimic.control;

import org.allsparks.mimic.config.InterlockInputs;
import org.allsparks.mimic.config.InterlockOutcome;
import org.allsparks.mimic.config.InterlockRule;
import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * One third-turn per accepted full pull. The pull is accepted only when
 * {@code diverter requires launcher READY} evaluates true. The ready bit is
 * {@link AttachmentContext#permitted()}. A pull while that bit is false is
 * spent and does not move the target. No home switch: zero is the caller's
 * encoder zero. This law does not write the motor.
 */
public final class DiscreteIndexLaw implements AttachmentLaw {
    private final double ticksPerRevolution;
    private final double indexPower;
    private final int settleTicks;
    private final InterlockRule readyRule;
    private boolean armed = true;
    private int indexes;
    private int target;

    public DiscreteIndexLaw(double ticksPerRevolution, double indexPower) {
        if (!Double.isFinite(ticksPerRevolution) || ticksPerRevolution <= 0.0) {
            throw new IllegalArgumentException("ticksPerRevolution must be > 0");
        }
        if (!Double.isFinite(indexPower) || indexPower <= 0.0 || indexPower > 1.0) {
            throw new IllegalArgumentException("indexPower must be in (0, 1]");
        }
        this.ticksPerRevolution = ticksPerRevolution;
        this.indexPower = indexPower;
        this.settleTicks = AttachmentMath.settleTicks(ticksPerRevolution);
        this.readyRule = InterlockRule.when("diverter")
                .requires("launcher", InterlockRule.READY)
                .onFail(InterlockOutcome.REJECT);
    }

    @Override
    public ActuatorCommand command(MechanismSnapshot snapshot, AttachmentGoal goal, AttachmentContext context) {
        if (goal != null && goal.kind() == AttachmentGoal.Kind.TRIGGER) {
            boolean ready = requirementMet(context);
            AttachmentMath.Pull pull = AttachmentMath.pull(armed, goal.value(), ready);
            armed = pull.armed;
            if (pull.index) {
                indexes = indexes + 1;
                target = AttachmentMath.indexTicks(indexes, ticksPerRevolution);
            }
        }
        return hold(snapshot);
    }

    @Override
    public void onStop() {
        armed = true;
    }

    private boolean requirementMet(AttachmentContext context) {
        boolean readyBit = context != null && context.permitted();
        InterlockInputs inputs = InterlockInputs.empty().withReady("launcher", readyBit);
        return readyRule.evaluate(inputs).accepted();
    }

    private ActuatorCommand hold(MechanismSnapshot snapshot) {
        double ticks = snapshot == null ? Double.NaN : snapshot.position();
        boolean arrived = indexes == 0 || AttachmentMath.nearTarget(ticks, target, settleTicks);
        double power = arrived ? 0.0 : indexPower;
        return ActuatorCommand.position(target, power);
    }
}
