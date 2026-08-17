# Tuning

Phase 0 has **no motion gains**. Tune observation first.

## What to graph

- position (documented units)
- velocity
- commanded vs applied output (identical in Phase 0)
- limit asserted bits
- `sensorValid`
- loop duration nanoseconds
- current (if polled; watch loop time)

## Flags

| Flag | Default | Effect |
|------|---------|--------|
| Phase 0 contracts | on | Observation session allowed |
| Phase 1 passive observation | off | Reserved for richer telemetry |
| Phase 2–10 | off | Must stay off until review |

`MimicSession` **throws** if any actuation flag is true.

## How to disable

Use `MimicFeatureFlags.defaults()` and do not call any future `setPower` helper. Team `setPower` remains your OpMode’s responsibility in Phase 0.

## Incorrect behavior

- Position sign flips when you jog “up” → fix `DirectionSign` before homing.
- Loop time jumps when enabling current polls → reduce poll rate ([research.md](research.md) GM0 note).
- CSV grows without bound in a long practice → logger capacity drops oldest events (`droppedCount`).
