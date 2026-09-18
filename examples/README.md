# Examples

These sketches show integration intent. They are **not** full FTC OpModes (no `hardwareMap` dependency in the library build).

## Phase 0 — observe only

1. Construct `MechanismUnits` with documented ticks-per-unit and direction.
2. Wire `RevMotorObserver` suppliers to `DcMotorEx` getters and the last commanded power.
3. Optionally wire `RevDigitalChannelObserver` to limit switches.
4. Call `MimicSession.observe()` once per loop.
5. Leave all `setPower` / `setVelocity` / servo position calls unchanged.

See [integration.md](../docs/mechanism-control/integration.md) and [mechanism-kinds.md](../docs/mechanism-control/mechanism-kinds.md). This year’s BumbleBee list lives in TeamCode (`mechanisms/README.md`), not in these sketches.

| Sketch | Family / construct |
|--------|--------------------|
| [intake/](intake/README.md) | Intake (roller) |
| [transfer/](transfer/README.md) | Transfer (feeder; internal path, not "shoot") |
| [launcher/](launcher/README.md) | Launcher energy + Arm aim (flywheel + turret/hood composition) |
| [elevator/](elevator/README.md) | Lift / elevator (generic linear axis) |
| [arm/](arm/README.md) | Arm (wrist / rotary pose; turret and hood are aim, not energy) |
| [end-effector/](end-effector/README.md) | End effector (claw named states; servo command is not pose) |
| [climber/](climber/README.md) | Climber (loaded hang; no auto-release, no homing under load) |
| [field-element/](field-element/README.md) | Field element (spinner / pusher / grabber of a field device) |
| [passive/](passive/README.md) | Passive (funnel or guide; zero actuators) |

These sketches are not [#68](https://github.com/The-Allsparks/MIMIC/issues/68) hardware acceptance cards and do not close [#6](https://github.com/The-Allsparks/MIMIC/issues/6).

## Later phases

Do not enable from examples until acceptance tests in [phases.md](../docs/mechanism-control/phases.md) pass and maintainers review. Active control stays behind [#69](https://github.com/The-Allsparks/MIMIC/issues/69).
