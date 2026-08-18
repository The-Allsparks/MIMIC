package org.allsparks.mimic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.allsparks.mimic.api.CalibrationState;
import org.allsparks.mimic.api.GoalResult;
import org.allsparks.mimic.api.MechanismStatus;
import org.allsparks.mimic.api.MimicMechanism;
import org.allsparks.mimic.log.MimicEvent;
import org.allsparks.mimic.log.MimicEventLogger;
import org.allsparks.mimic.log.MimicEventType;
import org.allsparks.mimic.observe.LoopOverheadStats;
import org.allsparks.mimic.observe.MechanismObserver;
import org.allsparks.mimic.observe.MechanismSnapshot;

/**
 * Per-OpMode MIMIC session for one mechanism. Observes and logs; never
 * commands motors or servos.
 *
 * Phase 0 always samples. Phase 1 extras (richer logging) run only when
 * {@link MimicFeatureFlags#isPhase1PassiveObservation()} is true.
 */
public final class MimicSession implements MimicMechanism<Double> {
    public static final String NO_ACTIVE_CONTROL = "NO_ACTIVE_CONTROL";

    private final MimicFeatureFlags flags;
    private final MechanismObserver observer;
    private final MimicEventLogger logger;
    private final LoopOverheadStats loopStats;
    private MechanismSnapshot lastSnapshot;
    private long samples;

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
     * Read sensors and update logs. Call once per robot loop. Does not write
     * motor or servo outputs.
     */
    public MechanismSnapshot observe() {
        MechanismSnapshot snapshot = observer.capture();
        samples++;
        loopStats.offer(snapshot.loopDurationNanos());
        logger.recordObservation(snapshot);
        lastSnapshot = snapshot;
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
        logger.record(new MimicEvent(timestamp, MimicEventType.GOAL_REJECTED, NO_ACTIVE_CONTROL, fields));
        return GoalResult.rejected(NO_ACTIVE_CONTROL);
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
     * Records a stop request. Phase 0 does not write hardware; tests must
     * confirm actuator write counts remain unchanged.
     */
    @Override
    public void stop() {
        long timestamp = lastSnapshot == null ? 0L : lastSnapshot.timestampNanos();
        logger.record(new MimicEvent(timestamp, MimicEventType.STOP_REQUESTED, "phase0_log_only", null));
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
}
