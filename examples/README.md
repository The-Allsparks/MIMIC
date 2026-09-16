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

## Later phases

Do not enable from examples until acceptance tests in [phases.md](../docs/mechanism-control/phases.md) pass and maintainers review.
