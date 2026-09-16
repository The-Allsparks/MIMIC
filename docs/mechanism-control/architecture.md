# MIMIC architecture

Write and review this architecture **before** enabling active mechanism control.

## Goals

- Mechanism lifecycle complementary to [ViDAR](https://github.com/The-Allsparks/ViDAR) field awareness, Pedro Pathing chassis motion, and [AMPER](https://github.com/The-Allsparks/AMPER) electrical policy.
- Phased enablement: observe → calibrate → limit → profile → synchronize → interlock → recover → coordinate power.
- Hardware-independent core with REV adapters today and a documented SystemCore boundary later.

## Responsibility boundary

MIMIC answers:

- Is the mechanism calibrated?
- Is the requested goal permitted?
- What safe motion path or profile should it use?
- Are sensors and actuators consistent?
- Continue, degrade, retry, or stop?
- What power request should AMPER see?
- What final output is safe to send?

MIMIC does **not** answer:

| Question | Owner |
|----------|-------|
| Where should the chassis drive? | Pedro Pathing |
| What is visible around the robot? | ViDAR |
| How should total robot power be allocated? | AMPER |
| What high-level scoring task is next? | Future behavior layer |
| How are all robot commands scheduled? | Ivy, NextFTC, or team scheduler |

## Module map

```text
OpMode / scheduler
    │
    ├─ read sensors once
    ├─ MechanismObserver ──► immutable MechanismSnapshot   (never writes)
    ├─ CalibrationManager / FaultMonitor                   (Phase 2 / 8)
    ├─ operator / autonomous intent
    ├─ GoalValidator + InterlockManager                    (Phase 3 / 7)
    ├─ MotionPlanner setpoints                             (Phase 4)
    ├─ MechanismController adapter                         (Phase 4)
    ├─ AmperPowerRequest ──► AMPER ──► AmperPowerGrant     (Phase 9)
    ├─ anti-windup / profile pause
    ├─ ActuatorSafetyGate                                  (Phase 3+)
    ├─ command hardware                                    (disabled in Phase 0/1)
    └─ MimicEventLogger + rate-limited telemetry
```

Phase 0/1 stop after snapshot + log. Team code still owns `setPower`.

## Library vs this year’s robot

MIMIC is reusable. Generic families live in `org.allsparks.mimic.templates`. Instance topology, sensor roles, and capability declarations live in `org.allsparks.mimic.config` (metadata only). Hardware maps, season game pieces, robot names, and named interlocks belong in TeamCode. See [library-vs-teamcode.md](library-vs-teamcode.md), [mechanism-kinds.md](mechanism-kinds.md), and [generic-mechanism-catalog.md](generic-mechanism-catalog.md).

## `MechanismObserver`

Captures once per loop: position, velocity, acceleration estimate, commanded/applied output, current where available, limits, absolute sensor, calibration observation, freshness, loop timing, redundant disagreement.

Optional named extras (`namedSample` / `namedLimit`) copy onto the snapshot by team-owned name and `SensorRole`. They do not replace the fixed channels and do not change `sensorValid`.

`sensorValid` is aggregate health of **required wired channels**, not “every channel exists.” Primary pose is required (omitted `ticks` keeps it false). Velocity is required only if `ticksPerSecond` is wired; `UNSUPPORTED` velocity does not clear the flag. Analog-only mechanisms wire the mapped analog value as `ticks` and omit velocity; `absoluteSensor` is an optional extra channel, not a substitute primary pose.

**Must not command hardware.**

## `MechanismSnapshot`

Immutable once-per-loop observation. Never used to write hardware. Position and velocity are `SensorSample`s (value, unit, and `MeasurementValidity`, including observer-liveness `STALE`). Scalar `position()` / `velocity()` delegate to the sample values. Absolute and redundant channels are also `SensorSample`s.

Named extras are additive. `sample("entry")` and `role(SensorRole.PIECE_ENTRY)` return a `RoleSample` with either a `SensorSample` or a `LimitSwitchSample`. Declared roles such as piece-entry live here; they have no first-class snapshot field. Missing names or roles are `UNSUPPORTED` on both sides, not a fake `false` / not-asserted reading. Do not invent values. `role(RELATIVE_POSITION)` / `role(VELOCITY)` / `role(RETRACT_LIMIT)` / `role(EXTEND_LIMIT)` / `role(ABSOLUTE_POSITION)` / `role(REDUNDANT_POSITION)` still expose the existing fields. Extra optional suppliers do not replace those fields.

`PieceObservation.from(snapshot)` is observe-only presence and count on those extras. `PIECE_ENTRY` / `PIECE_EXIT` report `VALID`, `MISSING`, or `UNSUPPORTED`. A missing sensor is unknown occupancy, not empty. A `VALID` count of `0` is known empty; an unwired count is unknown, not zero. Identity, capacity, and reconcile stay out ([#64](https://github.com/The-Allsparks/MIMIC/issues/64)). No actuation.

`Readiness.atSpeed(snapshot, minVel, hysteresis, dwellNanos)` is pure settling evaluation over velocity (position dual: `inTolerance`). One usable loop inside the band is not ready; dwell must elapse. Invalid velocity is not at-speed. `feed` returns a new evaluator and does not write hardware. Not a feeder interlock ([#59](https://github.com/The-Allsparks/MIMIC/issues/59)).

`StallDetector.update(snapshot)` is observe-only stall suspicion from current, velocity, and a required timeout. Missing / NaN current is unsupported, not stalled. `JamDetector` uses the same heuristic. Neither writes hardware, reverse-clears, nor is called by `MimicSession` ([#60](https://github.com/The-Allsparks/MIMIC/issues/60)).

`SyncContract` is an optional unused disagreement limit on independently sensed topology. Linked motors share a command; a contract on a common shaft is rejected. `actuatorCount > 1` does not imply sync. `MimicSession` does not call it. Phase 5 flags stay off. Anti-racking output and Allsparks elevator CAD stay later ([#61](https://github.com/The-Allsparks/MIMIC/issues/61), [#15](https://github.com/The-Allsparks/MIMIC/issues/15), [#16](https://github.com/The-Allsparks/MIMIC/issues/16)).

`InterlockRule` is a named unused contract: `when("feeder").requires("launcher", READY).onFail(REJECT)`. Table evaluation returns `GoalDisposition`. `MimicSession` does not register rules. Not a scheduler ([#62](https://github.com/The-Allsparks/MIMIC/issues/62)).

## `CalibrationManager` (Phase 2 — not implemented)

Owns homing strategy, direction, max output, max travel, timeout, debounce, encoder reset policy, completion, invalidation.

## `GoalValidator` (Phase 3+)

Accept / reject / defer / clamp / replace with named reasons.

## `MotionPlanner` (Phase 4)

Mechanism setpoints (not chassis paths): direct position, velocity, trapezoid or other bounded profiles justified in [motion-control.md](motion-control.md).

## `MechanismController` (Phase 4)

Replaceable: filter, feedback, feedforward, saturation, anti-windup. Core seam: `MechanismControllerAdapter.effort(snapshot, setpoint)` returns dimensionless effort and does not write hardware. `MimicSession` does not call it. NextControl / FTCLib / WPILib adapters are optional TeamCode or test implementations and not Gradle dependencies ([build-vs-adopt.md](build-vs-adopt.md), [motion-control.md](motion-control.md)).

## `InterlockManager` (Phase 7)

Engine that would command hardware: still [#19](https://github.com/The-Allsparks/MIMIC/issues/19). Not implemented. Do not add this type yet.

`InterlockRule` is the named contract: `when("feeder").requires("launcher", READY).onFail(REJECT)`. `evaluate(InterlockInputs)` returns `GoalDisposition` via `GoalResult`. `MimicSession` does not register rules. Not a Command, Subsystem, or Scheduler ([#62](https://github.com/The-Allsparks/MIMIC/issues/62), [interlocks.md](interlocks.md)).

## `FaultMonitor` (Phase 8)

Stale sensors, unexpected motion, no motion despite output, jumps, limit disagreement, actuator disagreement, timeout, calibration loss, stall suspicion. Observe-only `StallDetector` / `JamDetector` exist unused by session; reverse-clear and Phase 8 recovery stay unimplemented.

## `ActuatorSafetyGate` (Phase 3+)

Last direction-aware check **immediately before** hardware output. Must remain effective if a controller, command, scheduler, or AMPER grant is wrong.

Phase 0 does **not** implement this gate in a form that can write motors.

## `MimicEventLogger`

Records goals, calibration, setpoints, controller output, AMPER constraint, applied output, limits, faults, recovery, timing. CSV export; field names stable for a future TRACE sink. TRACE is not required now.

## Hardware abstraction

| Type | Role |
|------|------|
| `MechanismObserver` | Snapshot factory |
| REV adapters | Supplier-wired SDK reads |
| `FakeActuator` / fake sensors | Tests; write-count assertions |
| `MimicClock` | Testable time |
| `SystemCoreAdapterBoundary` | Unimplemented |

## Required control order (when actuation exists)

1. Read mechanism sensors once.
2. Create an immutable snapshot.
3. Update calibration and fault state.
4. Read operator or autonomous intent.
5. Validate goals and interlocks.
6. Generate the current motion-profile setpoint.
7. Calculate feedback and feedforward.
8. Submit the mechanism’s request to AMPER when enabled.
9. Apply the AMPER constraint.
10. Anti-windup or profile adjustment.
11. Final actuator safety gate.
12. Command hardware.
13. Record requested versus applied.
14. Publish rate-limited telemetry.

Scheduler notes: [integration.md](integration.md).

## Safety analysis (direction)

See [safety-model.md](safety-model.md) for the full hazard list. Architecture rule: **software must fail conservatively without an avoidable violent movement.** Missing measurements disable active control.
