package org.allsparks.mimic.control;

import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Pure command for one attachment. Returns a request. The safety gate
 * decides whether that request may reach the sink. Implementations must
 * not write motors or servos.
 */
public interface AttachmentLaw {
    /**
     * Command for this snapshot and goal. Must not be null. Must not touch
     * hardware.
     */
    ActuatorCommand command(MechanismSnapshot snapshot, AttachmentGoal goal, AttachmentContext context);

    /** Park state. Called when the session stops, including before the first snapshot. */
    void onStop();
}
