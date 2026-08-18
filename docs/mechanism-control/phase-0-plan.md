# Phase 0 — exact file-level plan

## Package layout

```text
src/main/java/org/allsparks/mimic/
  MimicPhase.java
  MimicFeatureFlags.java
  MimicSession.java          (observe / reject goals / log stop; never writes)
  api/                       MimicMechanism, GoalResult, GoalDisposition,
                             CalibrationState, MechanismStatus
  clock/                     MimicClock, SystemNanoClock
  units/                     DirectionSign, LinearDistanceUnit, AngularUnit,
                             MechanismUnits, UnitConverter
  observe/                   snapshots, observer, validator, loop stats
  log/                       MimicEvent, MimicEventType, MimicEventLogger
  adapters/rev/              RevMotorObserver, RevDigitalChannelObserver,
                             RevAnalogSensorObserver
  adapters/amper/            AmperPowerRequest, AmperPowerGrant (inert)
  adapters/future/           SystemCoreAdapterBoundary
  fake/                      FakeActuator, FakeLimitSwitch, FakeAbsoluteSensor,
                             FakeMechanismHardware
```

## Done in this scaffold

- [x] Interfaces and immutable snapshots
- [x] Units and direction conventions
- [x] Observer without motor writes
- [x] REV supplier adapters
- [x] Fake hardware with write-count assertions
- [x] Logger CSV export foundation
- [x] Feature flags (actuation off; session refuses actuation flags)
- [x] Unit tests including doc links
- [x] Docs + CI
- [x] Build-versus-adopt decision

## Next Phase 0/1 hardware tasks (issues)

1. Wire adapters to live `DcMotorEx` / `DigitalChannel` on a Control Hub.
2. Measure loop overhead with 0 / N current polls.
3. Record a validation log during low-power jog (team `setPower`, MIMIC observe-only).
4. Confirm unsupported paths return `UNSUPPORTED`/`MISSING` without throwing into OpMode.

## Explicit non-goals (stop for review)

- Homing that moves the mechanism
- Soft/hard limit enforcement that changes output
- PID / profile following
- Safety gate connected to `setPower`
- AMPER grants that clip motors
