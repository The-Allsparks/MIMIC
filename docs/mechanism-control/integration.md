# Integration

## Iterative OpModes

Call `mimic.periodic()` or `observe()` once per `loop()`, **after** PULSE `capture()` if a sampler owns Hub I/O, or after your own bulk-read otherwise, **before** or after your own `setPower` — Phase 0 does not care because it never writes. Keep sensor reads consistent (once per loop).

When a sampler owns Hub I/O (PULSE implements `InputRegistrar` / `InputValues`), TeamCode must call `declareInputs`, bind each `physicalDoubles()` / `physicalBooleans()` getter, then `readFrom`, **before** the sampler freeze. `capture()` then reads the snapshot and does not call `getCurrentPosition()` a second time. Without those calls, wired suppliers still read hardware so MIMIC remains usable without PULSE. Do not import `org.allsparks.pulse` from this library.

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

This library is a desktop `java-library`. Robot names, ports, and BIOBUZZ interlocks belong in TeamCode ([library-vs-teamcode.md](library-vs-teamcode.md)). Integration into `FtcRobotController` is a composite `includeBuild` like AMPER — not published to Maven yet. Do not add that includeBuild until a TeamCode class actually constructs `MimicSession`.
