# Mechanism families and constructs

MIMIC keeps **generic templates** for common FTC mechanisms. TeamCode chooses which ones exist, names the hardware, and later writes `setPower`.

A template is a catalog entry (`MechanismFamily`, `MechanismConstruct`, `MechanismBlueprint`). Configuration (`MechanismConfiguration`) adds actuator topology, sensor roles, and capability declarations. None of these objects write motors or servos.

Full research: [generic-mechanism-catalog.md](generic-mechanism-catalog.md). Gap vs code: [generic-mechanism-gap-matrix.md](generic-mechanism-gap-matrix.md).

## Better word than "shoot" for the path inside the robot

Use **transfer**.

- **Intake** acquires a piece from the field.
- **Transfer** moves that piece along a path inside the robot (belt, rollers, indexer, feeder).
- **Launcher** sends the piece off the robot (energy only).
- **Arm** aims or poses (turret, hood, wrist). Compose arm + launcher; do not treat aim as launch energy.

"Shoot" mixes those jobs and sounds like the launcher. "Conveyor" is one transfer construct, not the family. "Feeder" is the last transfer stage into a launcher.

## Families

| Family | Job | Typical constructs |
|--------|-----|--------------------|
| **Intake** | Acquire from the field | roller, compliant wheel, vectored, deployable, spatula, vacuum |
| **Transfer** | Internal path | belt, roller path, indexer, feeder, hopper |
| **Launcher** | Impart energy so a piece leaves the robot | flywheel, catapult, spinapult, puncher |
| **Lift** | Linear positioning | elevator, linear slide, capstan, extension, lead-screw |
| **Arm** | Rotary positioning | pivot, wrist, turret, hood, four-bar |
| **End effector** | Named-state tool | claw, gate, bucket, latch, hook, pusher |
| **Climber** | Hang while loaded | winch, hook deployer, ratchet hang |
| **Field element** | Actuate a field device | carousel, foundation grabber, beacon pusher, marker deployer |
| **Passive** | Guide or structure | funnel, deployable structure |

Storage/indexing is **not** a family. It is Transfer plus optional piece-tracking capability.

Java: `org.allsparks.mimic.templates` and `org.allsparks.mimic.config`.

Custom layouts: `ConstructDescriptor.custom("myLayout", family, motionKind, summary)` without editing the enum.

## Launcher vs aim

Launch **energy** and **aim** are often one scoring assembly, but they are separate MIMIC mechanisms (separate observers, separate ids).

| Construct | Family | Role |
|-----------|--------|------|
| Flywheel | Launcher | Spinning wheel that imparts velocity |
| Catapult | Launcher | Arm or pan that stores energy and throws |
| Spinapult | Launcher | Spinning arm that both stores and releases energy |
| Puncher | Launcher | Linear stored-energy shot |
| Turret | Arm | Yaw aiming axis |
| Hood | Arm | Launch-angle aiming axis |

Do not put season game-piece types or robot port names in these enums.

## Elevator

Elevator is a **lift construct**, not a season product. A team may never build one. The [elevator observation sketch](../../examples/elevator/README.md) is a generic linear axis. Historical lift research: [elevator-target.md](elevator-target.md). Cascade vs continuous rigging is conversion configuration, not a second controller.

## Configuration (Phase 0 metadata)

```java
MechanismConfiguration lift =
        MechanismConfiguration.builder("mainLift")
                .construct(ConstructDescriptor.standard(MechanismConstruct.ELEVATOR))
                .actuators(ActuatorTopology.independentlySensedMotors(2))
                .sensor("leftPosition", SensorRole.RELATIVE_POSITION)
                .sensor("rightPosition", SensorRole.REDUNDANT_POSITION)
                .sensor("home", SensorRole.RETRACT_LIMIT)
                .enable(Capability.HOMING)
                .enable(Capability.SOFT_LIMITS)
                .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                .controlDomain(ControlDomain.PROFILED_POSITION)
                .build();
```

Presets in `StandardPresets` are **suggestions**. They do not mean the sensor is wired. `build()` validates contradictions (for example homing with no home source) and never writes hardware.

## Starting from a preset

A student can start from a preset and delete sensors they do not have. Optional roles are extras, not wiring.

```java
PresetSuggestion suggestion = StandardPresets.suggestionFor(MechanismConstruct.ELEVATOR);
List<SensorRole> extras = suggestion.optionalRoles();
```

- `exampleSensors()` is the smallest set that can make a valid desktop example when those capabilities have evidence.
- `optionalRoles()` lists extras a team can add or delete (home already in the elevator example; upper limit, absolute, redundant, and current are extras). They are not HardwareMap names.
- `hazardNotes()` copies catalog hazard phrases. They are not a controller.
- Do not enable a capability just because the optional list mentions a matching sensor. `exampleConfiguration` copies example sensors and only capabilities that already have evidence.
- Suggestion lists do not auto-enable `MULTI_ACTUATOR_SYNCHRONIZATION`. A second position role, or `actuatorCount > 1`, still does not imply sync.

Keep only example sensors you actually have, then declare optional roles you wired. Delete the rest. Do not auto-wire `HardwareMap`.

`MechanismBlueprint` remains a thin id + construct catalog entry:

```java
MechanismBlueprint feeder = MechanismBlueprint.of("feeder", MechanismConstruct.FEEDER);
MechanismUnits units = MechanismUnits.rotaryRadians("feeder", ticksPerRadian, DirectionSign.POSITIVE);
```

Use the same `mechanismId` on the observer. Phase 0 still only observes. Which constructs this year's robot uses lives in TeamCode: [library-vs-teamcode.md](library-vs-teamcode.md).
