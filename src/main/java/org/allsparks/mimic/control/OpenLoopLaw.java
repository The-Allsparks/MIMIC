package org.allsparks.mimic.control;

import org.allsparks.mimic.observe.MechanismSnapshot;

/** Stick power. Does not read the encoder to decide the command. */
public final class OpenLoopLaw implements AttachmentLaw {
    @Override
    public ActuatorCommand command(MechanismSnapshot snapshot, AttachmentGoal goal, AttachmentContext context) {
        if (goal == null || goal.kind() != AttachmentGoal.Kind.EFFORT) {
            return ActuatorCommand.stop();
        }
        return ActuatorCommand.effort(goal.value());
    }

    @Override
    public void onStop() {
        // No latched edge. The next effort goal is the whole command.
    }
}
