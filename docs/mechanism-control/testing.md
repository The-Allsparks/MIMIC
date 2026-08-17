# Testing

## Unit tests (this scaffold)

Covered now: unit conversion, direction, observer snapshots, missing/disconnected sensors, disagreement, goal rejection, feature flags, AMPER unrestricted grant, loop overhead, **zero actuator writes**, documentation links.

Later phases must add: debounce, timeout, hard/soft limits, motion away from a limit, deferral, profiles, saturation, anti-windup, sync, interlocks, fault severity, bounded recovery, AMPER clip handling.

## Simulation tests (later)

Model: normal and counterbalanced elevator, wrong encoder direction, stuck/disconnected switch, frozen encoder, position jump, jam, lagging side, ratchet fail-to-release, gravity backdrive, saturation, weak-battery AMPER grant, interrupted transitions.

Do **not** deliberately create destructive jams or uncontrolled drops on hardware.

## Robot test cards (when hardware exists)

Each card requires: adult supervision, supports/restraints, exclusion zone, e-stop procedure, maximum initial output, immediate-stop criteria.

| Card | Immediate stop if |
|------|-------------------|
| Sensor direction | Motion disagrees with documented + axis |
| Limit switch | Switch does not change when manually pressed |
| Low-output jog | Binding, smoke, cable slack on loaded side |
| Homing | Timeout, runaway, bounce-home |
| Soft-limit approach | Overshoot past margin |
| Hard-limit response | Continues driving into switch |
| Profile tuning | Oscillation or impact |
| Holding | Drop or climb uncommanded |
| Ratchet | Engage under motion |
| Sync | Visible racking |
| Fault recovery | Repeated impacts |
| OpMode stop | Motion continues |
| Power interrupt | Violent restart motion |

## Student exercise (Phase 0, fake or recorded data)

1. Run `.\gradlew.bat test`.
2. Export CSV from `MimicEventLogger` in a unit test.
3. Graph `pos` vs time and mark a `SENSOR_INVALID` row.
4. Explain why `NO_ACTIVE_CONTROL` is the correct Phase 0 goal result.
