# Calibration and homing

**Phase 2 — not implemented.** Phase 0 only documents conventions and reports `UNCALIBRATED`. Declaring a `CalibrationContract` does not run homing.

## CalibrationContract (declaration-only)

A student should be able to list what a homing strategy must declare **before** anyone moves the mechanism. `CalibrationContract` is that object. It never writes motors or servos.

| Field | Meaning |
|-------|---------|
| Direction | Permitted homing direction (`DirectionSign`) |
| Max travel | Maximum distance a later homing move may travel |
| Timeout | Maximum time (nanoseconds) a later homing move may run |
| Debounce | How long a switch or index must stay asserted |
| Encoder reset policy | When a later phase may relabel origin (`ON_COMPLETION`, `ON_REFERENCE`, `OFFSET_ONLY`, or `UNSPECIFIED`) |

Builder sketch:

```java
CalibrationContract.homeSwitch().maxTravel(400.0).timeout(2_000_000_000L)
```

Attach optionally. Default is absent, so existing `.calibrationStrategy(...)` builders stay valid:

```java
MechanismConfiguration.builder("lift")
        .calibrationContract(
                CalibrationContract.homeSwitch()
                        .maxTravel(400.0)
                        .timeout(2_000_000_000L)
                        .build())
```

`ConfigurationValidator` rejects missing timeout or max travel when a homing-capable strategy (`HOME_SWITCH`, `INDEX_PULSE`, or `HARD_STOP_CURRENT`) has a contract. `HARD_STOP_CURRENT` stays blocked: declaring a contract does not enable current-home.

A contract is **not** permission to move:

- `CalibrationContract.permitsMotion()` is always false
- `MimicSession.calibrationState()` stays `UNCALIBRATED`
- Goals stay `NO_ACTIVE_CONTROL`
- `MimicFeatureFlags.isAnyActuationEnabled()` stays false with defaults

Running homing remains [#8](https://github.com/The-Allsparks/MIMIC/issues/8) / [#9](https://github.com/The-Allsparks/MIMIC/issues/9).

## Why this exists

Quadrature encoder ticks are a **delta** from whatever count was last called zero. REV recommends resetting in init and using a repeatable physical start pose.[^rev-enc] That is not the same as knowing the carriage is on the hard stop after a match collision.

## Strategies (declare all fields)

| Strategy | When it is justified |
|----------|----------------------|
| Limit switch | Repeatable edge, correct NO/NC, debounce |
| Absolute encoder | Legal sensor, mapped units, validity check |
| Known startup state | Mechanism cannot move while disabled **and** start pose is fixture-checked |
| Controlled hard-stop detect | Only if mechanically safe, rules-compliant, output-capped, timeout-bound |
| Redundant agreement | Switch + absolute, or two towers |

## Encoder reset policy

Resetting too early labels the wrong pose as zero. Resetting while still moving can leave a biased origin. Homing completion must specify **when** `STOP_AND_RESET_ENCODER` (or an equivalent offset) runs.

SDK reset also removes motor power as a side effect; brake vs float is unspecified.[^runmode]

## Invalidation

Lose calibration on: impossible jump, persistent redundant disagreement, missed expected switch, brownout/reboot, explicit driver reset.

[^rev-enc]: https://docs.revrobotics.com/duo-control/hello-robot-java/part-3/using-encoder
[^runmode]: https://javadoc.io/doc/org.firstinspires.ftc/RobotCore
