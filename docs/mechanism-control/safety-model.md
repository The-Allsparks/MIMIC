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
