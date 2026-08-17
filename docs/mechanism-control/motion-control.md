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

Implementations must be replaceable. Changing the controller must not change safety-gate or interlock logic.

## Gravity

- Elevator: approximately constant \(k_G\) **if** uncounterbalanced.
- Arm: \(k_G \cos\theta\).
- Counterbalanced elevator: measure; may be near zero or sign-flipping if the counterbalance fails ([elevator-target.md](elevator-target.md)).

## Anti-windup

When output saturates, AMPER clips, or the safety gate zeros a direction, freeze or back-calculate integral terms.

## Settling and timeout

A goal is complete when error and velocity are inside tolerance for N cycles, or it **times out** into a named fault — never run forever.

[^wpilib-trap]: https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/trapezoidal-profiles.html
