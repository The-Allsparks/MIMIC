# Motion control

**Phase 4 — not implemented.** This page is the design contract.

## Planner vs chassis pathing

`MotionPlanner` produces **mechanism** setpoints (mm or rad versus time). It is not Pedro Pathing.

## Preferred profiles

Start with **trapezoidal** velocity constraints (WPILib/FTCLib precedent).[^wpilib-trap] Other profiles need a written justification.

NextControl documents interpolators including trapezoids, but the trapezoidal PR was **not merged** on 2026-08-17. Do not assume `dev.nextftc:control` provides it. See [build-vs-adopt.md](build-vs-adopt.md).

## Controller adapter

```text
filtered measurement → profile reference → feedback + feedforward → saturated effort
```

Phase 4 needs that replaceable seam **without** pulling NextControl into MIMIC or inventing unvalidated PID / feedforward math in core.

Core types (Java 11, no records, no FTC SDK types):

- `Setpoint` — immutable instantaneous reference (`position`, `velocity`, `unitSymbol`). May differ from the goal. Declaring one is not motion.
- `MechanismControllerAdapter` — `double effort(MechanismSnapshot snap, Setpoint setpoint)`. Setpoint in, dimensionless effort out (`[-1, 1]`). No `setPower` / `setPosition`. No hardware.

`MimicSession` does **not** call the adapter. `MimicFeatureFlags.phase4ProfiledControl` stays off. `ControlDomain` (including `PROFILED_POSITION`) remains a configuration declaration, not a running controller.

There is no default PID in core. Do not add a WPILib-style feedforward “to fill the interface.” Fake adapters belong in unit tests and must not write `FakeActuator`. Production adapters belong in TeamCode (or a test), not as MIMIC Gradle dependencies.

**License:** NextControl is GPL-3.0. MIMIC is MIT. Linking NextControl into this library would infect the published artifact. A TeamCode-side adapter may wrap NextControl later, with a license warning, after [#10](https://github.com/The-Allsparks/MIMIC/issues/10) exists so adapter output cannot reach motors unchecked. [#12](https://github.com/The-Allsparks/MIMIC/issues/12) stays out of `build.gradle`.

Implementations must be replaceable. Changing the controller must not change safety-gate or interlock logic. See [build-vs-adopt.md](build-vs-adopt.md).

## Gravity

- Elevator: approximately constant \(k_G\) **if** uncounterbalanced.
- Arm: \(k_G \cos\theta\).
- Counterbalanced elevator: measure; may be near zero or sign-flipping if the counterbalance fails ([elevator-target.md](elevator-target.md)).

## Anti-windup

When output saturates, AMPER clips, or the safety gate zeros a direction, freeze or back-calculate integral terms.

## Settling and timeout

A goal is complete when error and velocity are inside tolerance for N cycles, or it **times out** into a named fault — never run forever.

[^wpilib-trap]: https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/trapezoidal-profiles.html
