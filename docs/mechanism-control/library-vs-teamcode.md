# Library vs TeamCode

MIMIC is a **reusable FTC mechanism library**. It must stay free of this year’s robot and game.

| Lives in MIMIC | Lives in TeamCode (or FORGE curriculum) |
|----------------|-----------------------------------------|
| `MimicSession`, snapshots, units, REV observers, fake hardware | Hardware map names, motor/servo objects, gear ratios |
| Generic families and constructs (`Intake`, `Transfer`, `Launcher`, `Lift`; catapult, spinapult, turret, elevator, …) | Which of those constructs this robot builds, and their ids |
| Generic interlock *engine* (when Phase 7 exists) | Named constraints: intake reject vs feeder, turret vs hood |
| Phase flags and observe-only policy | Which mechanisms exist on BumbleBee this season |
| Examples that use string ids like `"intake"` | BIOBUZZ roles (NECTAR reject, FLOWER tray, alliance color) |

TeamCode still calls `setPower` / servo writes in Phase 0/1. MIMIC observes.

Catalog of reusable layouts: [mechanism-kinds.md](mechanism-kinds.md). This year’s BumbleBee list: [FtcRobotController TeamCode mechanisms README](https://github.com/The-Allsparks/FtcRobotController/blob/bumblebee/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/mechanisms/README.md).

Do **not** add `BiobuzzIntake`, `NectarReject`, or BumbleBee config names to `src/main`. Extract a reusable class into MIMIC only after a second robot would need the same behavior.
