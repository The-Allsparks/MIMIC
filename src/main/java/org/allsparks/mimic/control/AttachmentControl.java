package org.allsparks.mimic.control;

import java.util.Objects;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * One attachment's law, gate, and sink. {@link #apply(MechanismSnapshot)} is
 * the only method that writes the sink, and only with the gate's command.
 * Constructing this object does not move hardware.
 */
public final class AttachmentControl {
    public static final String ATTACHMENT_GOAL = "ATTACHMENT_GOAL";
    public static final String NULL_GOAL = "NULL_GOAL";

    private final AttachmentLaw law;
    private final ActuatorSafetyGate gate;
    private final ActuatorSink sink;
    private final AttachmentContext context;
    private AttachmentGoal goal = AttachmentGoal.stop();

    public AttachmentControl(
            AttachmentLaw law, ActuatorSafetyGate gate, ActuatorSink sink, AttachmentContext context) {
        this.law = Objects.requireNonNull(law, "law");
        this.gate = Objects.requireNonNull(gate, "gate");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Remember the goal. Does not write the sink. A null goal is rejected
     * and the previous goal stays.
     */
    public GoalResult accept(AttachmentGoal goal) {
        if (goal == null) {
            return GoalResult.rejected(NULL_GOAL);
        }
        this.goal = goal;
        return GoalResult.accepted(ATTACHMENT_GOAL);
    }

    public AttachmentGoal goal() {
        return goal;
    }

    /**
     * Run the law, then the gate, then the sink. Call once per loop after
     * the snapshot has been captured.
     */
    public GateDecision apply(MechanismSnapshot snapshot) {
        ActuatorCommand requested = law.command(snapshot, goal, context);
        GateDecision decision = gate.allow(snapshot, requested);
        sink.write(decision.command());
        return decision;
    }

    /** Park through the sink. Rearms laws that latch a trigger edge. */
    public void forceStop() {
        goal = AttachmentGoal.stop();
        law.onStop();
        sink.write(ActuatorCommand.stop());
    }
}
