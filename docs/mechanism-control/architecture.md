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

## `MechanismObserver`

Captures once per loop: position, velocity, acceleration estimate, commanded/applied output, current where available, limits, absolute sensor, calibration observation, freshness, loop timing, redundant disagreement.

**Must not command hardware.**

## `MechanismSnapshot`

Immutable once-per-loop observation. Never used to write hardware. Position and velocity are `SensorSample`s (value, unit, and `MeasurementValidity`, including observer-liveness `STALE`). Scalar `position()` / `velocity()` delegate to the sample values. Absolute and redundant channels are also `SensorSample`s.

## `CalibrationManager` (Phase 2 — not implemented)

Owns homing strategy, direction, max output, max travel, timeout, debounce, encoder reset policy, completion, invalidation.

## `GoalValidator` (Phase 3+)

Accept / reject / defer / clamp / replace with named reasons.

## `MotionPlanner` (Phase 4)

Mechanism setpoints (not chassis paths): direct position, velocity, trapezoid or other bounded profiles justified in [motion-control.md](motion-control.md).

## `MechanismController` (Phase 4)

Replaceable: filter, feedback, feedforward, saturation, anti-windup. NextControl adapter is optional and not a Gradle dependency ([build-vs-adopt.md](build-vs-adopt.md)).

## `InterlockManager` (Phase 7)

Named constraints: calibration, geometry, other mechanisms, ratchet/brake, robot mode.

## `FaultMonitor` (Phase 8)

Stale sensors, unexpected motion, no motion despite output, jumps, limit disagreement, actuator disagreement, timeout, calibration loss, stall suspicion.

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
