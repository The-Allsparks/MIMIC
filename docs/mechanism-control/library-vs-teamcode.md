# Library vs TeamCode

MIMIC is a **reusable FTC mechanism library**. It must stay free of this year’s robot and game.

| Lives in MIMIC | Lives in TeamCode (or FORGE curriculum) |
|----------------|-----------------------------------------|
| `MimicSession`, snapshots, units, REV observers, fake hardware | Hardware map names, motor/servo objects, gear ratios |
| Generic families and constructs (`Intake`, `Transfer`, `Launcher`, `Lift`, `Arm`, …) and `MechanismConfiguration` | Which of those constructs this robot builds, ports, and their ids |
| `InterlockRule` contract and table evaluation (unused by session; engine [#19](https://github.com/The-Allsparks/MIMIC/issues/19)) | Which rules this robot names (feeder vs launcher, intake vs deployed) |
| `FaultPolicy` severity table (unused by session; recovery [#20](https://github.com/The-Allsparks/MIMIC/issues/20)) | Which roles declare `STOP_MECHANISM` vs `MARK_DEGRADED` on this robot |
| Phase flags and observe-only policy | Which mechanisms exist on BumbleBee this season |
| `MechanismControllerAdapter` / `Setpoint` (interface only; unused by `MimicSession`) | NextControl / FTCLib / PID implementations and license choice |
| Examples that use string ids like `"intake"` | BIOBUZZ roles (NECTAR reject, FLOWER tray, alliance color) |

TeamCode supplies the `ActuatorSink` for a bound attachment session. That sink is what calls `setPower` or `setVelocity`, and only with the command `ActuatorSafetyGate` allowed. Unbound sessions stay observe-only. Drive wheels are not in this path.

Catalog of reusable layouts: [mechanism-kinds.md](mechanism-kinds.md). This year’s BumbleBee list: [FtcRobotController TeamCode mechanisms README](https://github.com/The-Allsparks/FtcRobotController/blob/bumblebee/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/mechanisms/README.md).

Do **not** add `BiobuzzIntake`, `NectarReject`, or BumbleBee config names to `src/main`. Extract a reusable class into MIMIC only after a second robot would need the same behavior.
