package org.allsparks.mimic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.allsparks.mimic.api.CalibrationState;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.api.MechanismStatus;
import org.allsparks.mimic.api.MimicMechanism;
import org.allsparks.mimic.control.ActuatorCommand;
import org.allsparks.mimic.control.AttachmentControl;
import org.allsparks.mimic.control.AttachmentGoal;
import org.allsparks.mimic.control.GateDecision;
import org.allsparks.mimic.log.MimicEvent;
import org.allsparks.mimic.log.MimicEventLogger;
import org.allsparks.mimic.log.MimicEventSink;
import org.allsparks.mimic.log.MimicEventType;
import org.allsparks.mimic.observe.LoopOverheadStats;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Per-OpMode MIMIC session for one mechanism. An unbound session observes
 * and logs and does not write a sink. {@link #controlWith(AttachmentControl)}
 * is the reviewed path that commands one attachment: capture, law, safety
 * gate, then sink. Phase flags still do not enable that path.
 *
 * Phase 0 always samples. Phase 1 extras (richer logging) run only when
 * {@link MimicFeatureFlags#isPhase1PassiveObservation()} is true.
 * {@link #requestGoal(Double)} stays rejected. Attachment goals use
 * {@link #requestAttachment(AttachmentGoal)}.
 */
public final class MimicSession implements MimicMechanism<Double> {
    public static final String NO_ACTIVE_CONTROL = "NO_ACTIVE_CONTROL";

    private final MimicFeatureFlags flags;
    private final MechanismObserver observer;
    private final MimicEventLogger logger;
    private final LoopOverheadStats loopStats;
    private MimicEventSink eventSink = MimicEventSink.NOOP;
    private AttachmentControl control;
    private MechanismSnapshot lastSnapshot;
    private long samples;
    private AttachmentGoal loggedGoal;

    public MimicSession(MimicFeatureFlags flags, MechanismObserver observer, MimicEventLogger logger) {
        this.flags = Objects.requireNonNull(flags, "flags");
        this.observer = Objects.requireNonNull(observer, "observer");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.loopStats = new LoopOverheadStats();
        if (flags.isAnyActuationEnabled()) {
            throw new IllegalArgumentException(
                    "Phase 0/1 sessions cannot enable actuation flags. Active control requires review.");
        }
    }

    public static MimicSession create(MechanismObserver observer) {
        return new MimicSession(MimicFeatureFlags.defaults(), observer, new MimicEventLogger(2048));
    }

    /**
     * Install the law, gate, and sink used by later {@link #observe()} calls.
     * Does not write hardware by itself. Phase flags are not an alternative
     * to this call.
     */
    public MimicSession controlWith(AttachmentControl control) {
        this.control = Objects.requireNonNull(control, "control");
        return this;
    }

    /**
     * Read sensors and update logs. Call once per robot loop. A session with
     * {@link #controlWith(AttachmentControl)} then writes the gated command.
     * An unbound session does not.
     */
    public MechanismSnapshot observe() {
        MechanismSnapshot snapshot = observer.capture();
        samples++;
        loopStats.offer(snapshot.loopDurationNanos());
        notifySink(logger.recordObservation(snapshot));
        lastSnapshot = snapshot;
        if (control != null) {
            applyControl(snapshot);
        }
        return snapshot;
    }

    @Override
    public MechanismSnapshot snapshot() {
        return lastSnapshot;
    }

    @Override
    public CalibrationState calibrationState() {
        return CalibrationState.UNCALIBRATED;
    }

    @Override
    public GoalResult requestGoal(Double goal) {
        long timestamp = lastSnapshot == null ? 0L : lastSnapshot.timestampNanos();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("goal", goal == null ? "null" : Double.toString(goal));
        emit(new MimicEvent(timestamp, MimicEventType.GOAL_REJECTED, NO_ACTIVE_CONTROL, fields));
        return GoalResult.rejected(NO_ACTIVE_CONTROL);
    }

    /**
     * Store one attachment goal for the next {@link #observe()}. Unbound
     * sessions reject it and do not write. A repeated goal is not logged again.
     */
    public GoalResult requestAttachment(AttachmentGoal goal) {
        long timestamp = lastSnapshot == null ? 0L : lastSnapshot.timestampNanos();
        if (control == null) {
            emit(new MimicEvent(timestamp, MimicEventType.GOAL_REJECTED, NO_ACTIVE_CONTROL, null));
            return GoalResult.rejected(NO_ACTIVE_CONTROL);
        }
        GoalResult result = control.accept(goal);
        if (!result.accepted()) {
            emit(new MimicEvent(timestamp, MimicEventType.GOAL_REJECTED, result.reason(), null));
            return result;
        }
        if (loggedGoal == null || !loggedGoal.sameRequest(goal)) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("kind", goal.kind().name());
            fields.put("value", Double.toString(goal.value()));
            emit(new MimicEvent(timestamp, MimicEventType.GOAL_ACCEPTED, result.reason(), fields));
            loggedGoal = goal;
        }
        return result;
    }

    @Override
    public MechanismStatus status() {
        if (lastSnapshot == null) {
            return MechanismStatus.OBSERVING;
        }
        return lastSnapshot.sensorValid() ? MechanismStatus.OBSERVING : MechanismStatus.DEGRADED;
    }

    @Override
    public void periodic() {
        observe();
    }

    /**
     * Records a stop request. An unbound session does not write. A bound
     * session writes {@link ActuatorCommand#stop()} through the sink.
     */
    @Override
    public void stop() {
        long timestamp = lastSnapshot == null ? 0L : lastSnapshot.timestampNanos();
        if (control != null) {
            control.forceStop();
            loggedGoal = AttachmentGoal.stop();
            emit(new MimicEvent(timestamp, MimicEventType.STOP_REQUESTED, "gated_stop", null));
            return;
        }
        emit(new MimicEvent(timestamp, MimicEventType.STOP_REQUESTED, "phase0_log_only", null));
    }

    /**
     * TRACE / tests implement {@link MimicEventSink}. The in-memory ring stays.
     * Null or omitted keeps {@link MimicEventSink#NOOP} so observation maps are
     * still built for CSV, but TRACE is not called.
     */
    public MimicSession eventSink(MimicEventSink eventSink) {
        this.eventSink = eventSink == null ? MimicEventSink.NOOP : eventSink;
        return this;
    }

    public MimicEventSink eventSink() {
        return eventSink;
    }

    public MimicEventLogger logger() {
        return logger;
    }

    public LoopOverheadStats loopOverhead() {
        return loopStats;
    }

    public MimicFeatureFlags featureFlags() {
        return flags;
    }

    public long sampleCount() {
        return samples;
    }

    private void applyControl(MechanismSnapshot snapshot) {
        GateDecision decision = control.apply(snapshot);
        if (decision.passed()) {
            return;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("reason", decision.reason());
        fields.put("kind", decision.command().kind().name());
        emit(new MimicEvent(snapshot.timestampNanos(), MimicEventType.SAFETY_GATE, decision.reason(), fields));
    }

    private void emit(MimicEvent event) {
        logger.record(event);
        notifySink(event);
    }

    private void notifySink(MimicEvent event) {
        if (eventSink == MimicEventSink.NOOP) {
            return;
        }
        eventSink.onEvent(event);
    }
}
