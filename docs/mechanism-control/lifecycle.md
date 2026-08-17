# Lifecycle

## Calibration states

| State | Meaning | Position goals |
|-------|---------|----------------|
| `UNCALIBRATED` | Encoder origin is not a trusted pose | Reject (Phase 2+); Phase 0 reports this always |
| `HOMING` | Executing a declared homing strategy | Reject other goals |
| `CALIBRATED` | Origin and units trusted | May accept |
| `CALIBRATION_SUSPECT` | Disagreement, unexpected jump, or missed switch | Degrade; do not pretend to know pose |
| `FAULTED` | Unsafe to move | Stop per fault policy |

Phase 0 `MimicSession.calibrationState()` is always `UNCALIBRATED`.

## Semantic states (Phase 6)

`STOWED`, `INTAKING`, `CARRYING`, `SCORING`, `HOMING`, `RECOVERING`, `FAULTED` request **goals**, they do not write motor power.

Illegal transitions must fail with a named reason. Interrupted transitions leave a defined safe state (usually hold or stow-prep, mechanism-specific).

## Homing contract (Phase 2)

Every strategy declares:

- permitted direction
- maximum homing output
- maximum travel
- timeout
- completion condition
- debounce
- encoder reset behavior
- failure state
- allowed recovery

Homing must not run indefinitely.

## OpMode stop

FTC already removes power when an OpMode stops. MIMIC `stop()` in Phase 0 only logs `STOP_REQUESTED`. Later phases must still never bypass Hub failsafe paths.
