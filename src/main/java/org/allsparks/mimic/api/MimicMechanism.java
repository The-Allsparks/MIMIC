package org.allsparks.mimic.api;

import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Shared lifecycle surface for a mechanism. Implementations compose sensors,
 * observers, and later safety/control objects rather than inheriting a giant
 * universal class.
 *
 * {@link #periodic()} and {@link #stop()} must not command hardware in Phase 0.
 *
 * @param <G> goal type for this mechanism (position, named pose, etc.)
 */
public interface MimicMechanism<G> {
    MechanismSnapshot snapshot();

    CalibrationState calibrationState();

    GoalResult requestGoal(G goal);

    MechanismStatus status();

    void periodic();

    void stop();
}
