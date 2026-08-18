# Calibration and homing

**Phase 2 — not implemented.** Phase 0 only documents conventions and reports `UNCALIBRATED`.

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
