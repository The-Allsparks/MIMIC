# Fault handling

**Phase 8 recovery is not implemented.** Reverse-clear and other recovery motion stay forbidden.

## Policy table versus Phase 8 recovery

`FaultPolicy.severity(FaultKind.SENSOR_DISAGREEMENT, DegradedBehavior.STOP_MECHANISM)` is a lookup: catalog fault kind plus the per-role `DegradedBehavior` declaration onto a documented severity. It does not move the mechanism.

| This slice (policy table) | Later (Phase 8 / [#20](https://github.com/The-Allsparks/MIMIC/issues/20)) |
|---------------------------|--------------------------------------------------------------------------|
| Named kinds and default severities | Detectors that raise those kinds on live snapshots |
| Combine kind + `DegradedBehavior` | Bounded retry, reduced speed, re-home, reverse-clear |
| Latch unknown does not auto-release | Recovery motion that could disengage a latch |
| `permitsMotion()` / `permitsRecoveryMotion()` false | Any write that zeros or reverses an actuator |
| AMPER grant named as `INSUFFICIENT_AMPER_GRANT` | Grant faults that change output ([#21](https://github.com/The-Allsparks/MIMIC/issues/21)) |

Do not enable `MimicFeatureFlags.phase8Faults`. `MimicSession` does not call `FaultPolicy`. Session `status()` is still `DEGRADED` on an invalid snapshot only, not `FAULTED` from this table.

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

Catalog defaults use `INFO`, `DEGRADED`, and `STOP_MECHANISM`. `STOP_DEPENDENCIES` and `STOP_ROBOT` are named so students can read the scale. This table does not request a full robot stop.

## Policy table

`DegradedBehavior` maps first: `IGNORE_OPTIONAL` to `INFO`, `MARK_DEGRADED` and `DISABLE_CAPABILITY` to `DEGRADED`, `STOP_MECHANISM` to `STOP_MECHANISM`. Optional-sensor kinds (`SENSOR_INVALID`, `SENSOR_DISAGREEMENT`) may stay at `INFO` when the role is `IGNORE_OPTIONAL`. Other kinds floor at their default, so ignore cannot hide stall, jam, or latch unknown.

Required sensors cannot use `IGNORE_OPTIONAL` (`ConfigurationValidator.REQUIRED_SENSOR_IGNORE_OPTIONAL`).

| Fault kind | Catalog name | Default | `IGNORE_OPTIONAL` | `MARK_DEGRADED` / `DISABLE_CAPABILITY` | `STOP_MECHANISM` |
|------------|--------------|---------|-------------------|----------------------------------------|------------------|
| `SENSOR_INVALID` | invalid | `DEGRADED` | `INFO` | `DEGRADED` | `STOP_MECHANISM` |
| `SENSOR_DISAGREEMENT` | disagree | `DEGRADED` | `INFO` | `DEGRADED` | `STOP_MECHANISM` |
| `UNEXPECTED_LIMIT` | unexpected limit | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `STALL` | stall | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `JAM` | jam | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `SKEW` | skew | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `FAILED_HOME` | failed home | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `TIMEOUT` | timeout | `DEGRADED` | `DEGRADED` | `DEGRADED` | `STOP_MECHANISM` |
| `UNEXPECTED_MOTION` | unexpected motion | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `LOST_CALIBRATION` | lost calibration | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |
| `INSUFFICIENT_AMPER_GRANT` | insufficient AMPER grant | `DEGRADED` | `DEGRADED` | `DEGRADED` | `STOP_MECHANISM` |
| `LATCH_UNKNOWN` | latch unknown | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` | `STOP_MECHANISM` |

`FaultPolicy.autoReleases(LATCH_UNKNOWN)` is always false. A missing latch confirmation is not "disengaged." Do not auto-release.

`INSUFFICIENT_AMPER_GRANT` is declared only. It does not clip or zero motors ([#21](https://github.com/The-Allsparks/MIMIC/issues/21)).

Student check: if a redundant encoder disagrees, read that role's `DegradedBehavior`. `STOP_MECHANISM` means stop this mechanism later; `MARK_DEGRADED` means keep going with reduced trust. The table is the answer. Do not guess.

## Bounded recovery

Automatic retry, reduced speed, re-home, driver-confirmed recovery, pit-only reset, disable until reboot.

Unresolved physical faults cannot be hidden by clearing a flag. Retries are counted. Fault history is retained. Recovery must not hammer a hard stop. This document does not authorize reverse-clear.

## Phase 0

Invalid sensing is logged (`SENSOR_INVALID`) and status becomes `DEGRADED`. No recovery motion. `FaultPolicy` does not change that session rule.
