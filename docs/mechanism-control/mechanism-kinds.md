# Mechanism families and constructs

MIMIC keeps **generic templates** for common FTC mechanisms. TeamCode chooses which ones exist, names the hardware, and later writes `setPower`.

A template is a catalog entry (`MechanismFamily`, `MechanismConstruct`, `MechanismBlueprint`). It is **not** a controller and it never writes motors or servos.

## Better word than "shoot" for the path inside the robot

Use **transfer**.

- **Intake** acquires a piece from the field.
- **Transfer** moves that piece along a path inside the robot (belt, rollers, indexer, feeder).
- **Launcher** sends the piece off the robot.

"Shoot" mixes those jobs and sounds like the launcher. "Conveyor" is one transfer construct, not the family. "Feeder" is the last transfer stage into a launcher.

## Families

| Family | Job | Typical constructs |
|--------|-----|--------------------|
| **Intake** | Acquire from the field | roller, claw, spatula |
| **Transfer** | Internal path | belt, roller path, indexer, feeder |
| **Launcher** | Off the robot (energy + aim) | flywheel, catapult, spinapult, turret, hood |
| **Lift** | Translate a carriage, stage, or tray | elevator, linear slide, capstan, extension |

Java: `org.allsparks.mimic.templates`.

## Launcher constructs

Launch **energy** and **aim** are often one scoring assembly, but they are separate MIMIC mechanisms (separate observers, separate ids).

| Construct | Role |
|-----------|------|
| Flywheel | Spinning wheel that imparts velocity |
| Catapult | Arm or pan that stores energy and throws |
| Spinapult | Spinning arm that both stores and releases energy |
| Turret | Yaw aim, often composed with a launch-energy construct |
| Hood | Launch-angle aim, often composed with a launch-energy construct |

Do not put BIOBUZZ piece types or BumbleBee port names in these enums.

## Elevator

Elevator is a **lift construct**, not a season product. A team may never build one. The [elevator observation sketch](../../examples/elevator/README.md) is a generic linear axis. Historical lift research: [elevator-target.md](elevator-target.md).

## TeamCode fills in

```java
MechanismBlueprint feeder = MechanismBlueprint.of("feeder", MechanismConstruct.FEEDER);
MechanismUnits units = MechanismUnits.rotaryRadians("feeder", ticksPerRadian, DirectionSign.POSITIVE);
```

Use the same `mechanismId` on the observer. Phase 0 still only observes. Which constructs BumbleBee uses this year lives in TeamCode, not here: [library-vs-teamcode.md](library-vs-teamcode.md).
