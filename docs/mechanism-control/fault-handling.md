# Fault handling

**Phase 8 recovery is not implemented.** Reverse-clear and other recovery motion stay forbidden.

## Observe-only stall and jam

`StallDetector.update(snapshot)` returns `StallSuspicion`: suspected, not suspected, or unsupported. Sketch: current plus no motion for a timeout, not a single current sample.

Timeout is required (`timeoutNanos > 0`). One qualifying loop is not suspected. After the window elapses with usable high current and no motion, the verdict is suspected.

Missing, NaN, or unwired current is **unsupported, not stalled**. Unusable velocity is also unsupported, because the detector cannot confirm no motion. Current-only is not a hard limit and does not zero motors.

`JamDetector` / `JamSuspicion` use the same heuristic. Neither type writes hardware. `MimicSession` does not call them. Do not enable `MimicFeatureFlags.phase8Faults`; that flag is treated as actuation.

Debounce is a different change and is not used here. Bounded automatic jam clearing ([#20](https://github.com/The-Allsparks/MIMIC/issues/20)) remains out of scope.

## Detectors (design)

- output without expected motion
- motion without expected output
- impossible sensor jumps
- limit disagreement
- actuator desynchronization
- prolonged target error
- stall patterns (current + no motion + timeout): observe-only `StallDetector` / `JamDetector` now; reverse-clear still forbidden
- stale data
- calibration loss
- repeated timeout

## Severity

| Level | Meaning |
|-------|---------|
| `INFO` | Log only |
| `DEGRADED` | Reduced speed or disabled optional DOF |
| `STOP_MECHANISM` | Zero this mechanism (hold policy if gravity-critical) |
| `STOP_DEPENDENCIES` | Stop coupled mechanisms |
| `STOP_ROBOT` | Request full stop; never bypass FTC e-stop |

## Bounded recovery

Automatic retry, reduced speed, re-home, driver-confirmed recovery, pit-only reset, disable until reboot.

Unresolved physical faults cannot be hidden by clearing a flag. Retries are counted. Fault history is retained. Recovery must not hammer a hard stop. This document does not authorize reverse-clear.

## Phase 0

Invalid sensing is logged (`SENSOR_INVALID`) and status becomes `DEGRADED`. No recovery motion.
