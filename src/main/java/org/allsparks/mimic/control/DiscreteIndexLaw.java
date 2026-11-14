package org.allsparks.mimic.control;

import org.allsparks.mimic.config.InterlockInputs;
import org.allsparks.mimic.config.InterlockOutcome;
import org.allsparks.mimic.config.InterlockRule;
import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * One third-turn per accepted full pull. The pull is accepted only when
 * {@code diverter requires launcher READY} evaluates true. The ready bit is
 * {@link AttachmentContext#permitted()}. A pull while that bit is false is
 * spent and does not move the target. A {@link AttachmentGoal.Kind#STEP} moves
 * one third-turn without that ready bit, at {@code stepPower}. No home switch:
 * zero is the caller's encoder zero. This law does not write the motor.
 */
public final class DiscreteIndexLaw implements AttachmentLaw {
    private final double ticksPerRevolution;
    private final double indexPower;
    private final double stepPower;
    private final int settleTicks;
    private final InterlockRule readyRule;
    private boolean armed = true;
    private boolean stepConsumed;
    private boolean commanded;
    private int indexes;
    private int target;
    private double movePower;

    public DiscreteIndexLaw(double ticksPerRevolution, double indexPower) {
        this(ticksPerRevolution, indexPower, indexPower);
    }

    public DiscreteIndexLaw(double ticksPerRevolution, double indexPower, double stepPower) {
        if (!Double.isFinite(ticksPerRevolution) || ticksPerRevolution <= 0.0) {
            throw new IllegalArgumentException("ticksPerRevolution must be > 0");
        }
        if (!Double.isFinite(indexPower) || indexPower <= 0.0 || indexPower > 1.0) {
            throw new IllegalArgumentException("indexPower must be in (0, 1]");
        }
        if (!Double.isFinite(stepPower) || stepPower <= 0.0 || stepPower > 1.0) {
            throw new IllegalArgumentException("stepPower must be in (0, 1]");
        }
        this.ticksPerRevolution = ticksPerRevolution;
        this.indexPower = indexPower;
        this.stepPower = stepPower;
        this.movePower = indexPower;
        this.settleTicks = AttachmentMath.settleTicks(ticksPerRevolution);
        this.readyRule = InterlockRule.when("diverter")
                .requires("launcher", InterlockRule.READY)
                .onFail(InterlockOutcome.REJECT);
    }

    @Override
    public ActuatorCommand command(MechanismSnapshot snapshot, AttachmentGoal goal, AttachmentContext context) {
        if (goal != null && goal.kind() == AttachmentGoal.Kind.STEP) {
            if (!stepConsumed) {
                acceptStep(goal.value() < 0.0 ? -1 : 1, stepPower);
                stepConsumed = true;
            }
        } else {
            stepConsumed = false;
            if (goal != null && goal.kind() == AttachmentGoal.Kind.TRIGGER) {
                boolean ready = requirementMet(context);
                AttachmentMath.Pull pull = AttachmentMath.pull(armed, goal.value(), ready);
                armed = pull.armed;
                if (pull.index) {
                    acceptStep(1, indexPower);
                }
            }
        }
        return hold(snapshot);
    }

    private void acceptStep(int sign, double power) {
        indexes = indexes + sign;
        target = AttachmentMath.indexTicks(indexes, ticksPerRevolution);
        movePower = power;
        commanded = true;
    }

    @Override
    public void onStop() {
        armed = true;
        stepConsumed = false;
    }

    private boolean requirementMet(AttachmentContext context) {
        boolean readyBit = context != null && context.permitted();
        InterlockInputs inputs = InterlockInputs.empty().withReady("launcher", readyBit);
        return readyRule.evaluate(inputs).accepted();
    }

    private ActuatorCommand hold(MechanismSnapshot snapshot) {
        double ticks = snapshot == null ? Double.NaN : snapshot.position();
        boolean arrived = !commanded || AttachmentMath.nearTarget(ticks, target, settleTicks);
        double power = arrived ? 0.0 : movePower;
        return ActuatorCommand.position(target, power);
    }
}
