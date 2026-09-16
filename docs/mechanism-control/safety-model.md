# Safety model

Software must fail conservatively without causing an avoidable violent movement. **MIMIC does not advertise production safety.** This document is a hazard analysis, not a certification.

## Mechanical first

- Software limits do not replace physical hard stops where those are necessary.
- Incorrect homing, units, direction, or gearing can damage the mechanism.
- No active phase without mechanism-specific testing.
- Adult supervision for robot tests; exclusion zones; e-stop procedure in [testing.md](testing.md).

## Hazard register

| Hazard | Conservative software response | Phase |
|--------|--------------------------------|-------|
| Incorrect motor direction | Observation graphs first; do not home | 0–2 |
| Incorrect encoder direction | Disagreement / unexpected motion → suspect calibration | 1–2 |
| Wrong gearing / units | Explicit `MechanismUnits`; reject non-positive ticks/unit | 0 |
| Encoder reset at wrong pose | Homing policy + invalidation | 2 |
| NO vs NC switch | Documented invert flag; disconnected NC looks asserted | 0–3 |
| Switch bounce | Debounce before CALIBRATED | 2 |
| Disconnected / stale sensors | `MISSING`/`STALE`; disable actuation | 0+ |
| PID integral windup | Freeze/back-calculate when saturated or AMPER-clipped | 4 / 9 |
| Gravity compensation wrong | Never assume depower is safe; `gravityCritical` | 4 / 9 |
| BRAKE vs FLOAT | Document per mechanism; FLOAT can drop a load | 3 |
| Ratchet engage under motion | Interlock: no downward power until released | 7 |
| Stored energy (springs, CB) | Mechanical design; software cannot “turn off” energy | all |
| Unexpected backdrive | Hold policy + brake/ratchet | 3–8 |
| Multi-stage elevator | Travel maps per stage; do not invent CAD | 5 |
| Asymmetric loading | Sync monitor; shutdown not unlimited correction | 5 |
| Stall detection latency | Timeout + current; do not instant-jam-test | 8 |
| Silent full override | Forbidden; pit override is conspicuous | 3 |
| OpMode stop / exception | Never catch-and-continue motion; fail safe | all |
| Hub reboot / comms loss | Stale samples; no invented holds | 0+ |

## Override levels (Phase 3)

1. Normal manual / jog  
2. Restricted recovery  
3. Pit-only expert, obvious in telemetry  

Override must not disable the final safety gate without an explicit, logged reason.

## Final gate invariant

If a controller, scheduler, or AMPER grant requests motion **into** an asserted hard limit, the gate blocks that direction. Motion **away** from the limit may remain available.

## LimitContract versus the Phase 3 gate

`LimitContract` is a Phase 0 declaration plus pure predicates. It records soft bounds, stopping margin, wrap policy, and the missing-switch rule. Attach it optionally on `MechanismConfiguration`; existing policy-only builders stay valid because the contract defaults to absent.

It is **not** the ActuatorSafetyGate ([#10](https://github.com/The-Allsparks/MIMIC/issues/10)). It does not clamp output, does not call `setPower`, and is not consulted by `MimicSession`. The Phase 3 gate and jog modes ([#11](https://github.com/The-Allsparks/MIMIC/issues/11)) will consume a contract later. Declaring one is not enabling that gate.

Keep three ideas apart on paper:

| Idea | Meaning |
|------|---------|
| Target | Where you want the mechanism to be |
| Soft limit | Software pose bound plus stopping margin; slow or stop before the hard stop |
| Hard limit | Physical switch or stop that must not be driven further into |

`LimitContract.permitsMotion()` is always false.

## Missing switch is not clear

`LimitSwitchSample.missing()` stores `asserted = false` because a Java boolean needs a value. That `false` is not a measurement of "not at the limit." A `MISSING` sample **must not** authorize travel into that limit. `STALE`, `OUT_OF_RANGE`, and `DISAGREEING` are likewise not a clear switch.

`UNSUPPORTED` means no switch is wired on that channel. That is not a missing reading. Soft-only pose bounds may still be used. Hard policies still refuse `UNSUPPORTED` because a hard limit needs a usable switch. Only a `VALID` sample that is not asserted can pass the switch check.

## Wrap-aware rotary bounds

Linear lifts use `WrapPolicy.NONE`: the allowed set is the closed interval `[min, max]`. Pose is not reduced modulo a period.

Rotary axes (turret, hood, a joint whose cable cannot spin forever) use `WrapPolicy.WRAP_AWARE` and a positive period in the same units as the bounds (for example 360 degrees or 2π radians). `min` and `max` are an **allowed arc** measured in the positive direction on that circle:

- If `min <= max`, the arc does not cross 0 (example: 10 to 350 with the wrap stop through 0).
- If `min > max`, the arc crosses 0 (example: 350 to 10 is the short window through 0).

The complementary arc is the forbidden wrap stop. Pose is reduced modulo the period before the inside-arc test. Stopping margin shrinks the allowed arc from both ends along the circle. This is not shortest-path planning and does not unwrap a continuous encoder. It only answers whether travel into a named bound is authorized.
