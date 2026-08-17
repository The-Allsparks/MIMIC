# Integration

## Iterative OpModes

Call `mimic.periodic()` or `observe()` once per `loop()`, **after** bulk-reading hardware if you use bulk reads, **before** or after your own `setPower` — Phase 0 does not care because it never writes. Keep sensor reads consistent (once per loop).

## Linear OpModes

Call `observe()` inside `while (opModeIsActive())`. Avoid blocking `sleep` that starves snapshots if you later enable control.

## Ivy / NextFTC / FTCLib commands / custom schedulers

MIMIC is **not** a scheduler. A subsystem `periodic()` should:

1. snapshot
2. (later) validate / plan / control / gate
3. leave command composition to Ivy, NextFTC, FTCLib, or team code

NextFTC command examples that set `motor.power` from NextControl remain valid; wrap them with MIMIC observation first, then later with the safety gate **around** the power write.

## Pedro Pathing and ViDAR

No coupling in Phase 0. Future interlocks may read chassis pose or “camera blocked” flags as **inputs**, not as MIMIC-owned planners.

## AMPER

See [amper-integration.md](amper-integration.md). Phase 0 types are inert.

## Copying into TeamCode

This library is a desktop `java-library`. Integration into `FtcRobotController` is a composite/source copy like AMPER — not published to Maven yet.
