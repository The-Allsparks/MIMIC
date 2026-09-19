# Launcher observation sketch (Phase 0)

**Launcher** family is launch energy only (flywheel, catapult, spinapult, puncher). Aiming axes (turret, hood) are **Arm** family constructs composed with a launcher. Give each axis its own blueprint and observer.

Passive observation only. TeamCode still owns `setPower`.

```java
MechanismBlueprint flywheel = MechanismBlueprint.of("flywheel", MechanismConstruct.FLYWHEEL);
MechanismBlueprint turret = MechanismBlueprint.of("turret", MechanismConstruct.TURRET);
MechanismBlueprint hood = MechanismBlueprint.of("hood", MechanismConstruct.HOOD);

MechanismUnits flywheelUnits = MechanismUnits.rotaryRadians(
        flywheel.mechanismId(),
        /* ticksPerRadian */ 1.000,
        DirectionSign.POSITIVE);

RevMotorObserver flywheelAdapter = RevMotorObserver.create(
        flywheel.mechanismId(),
        new SystemNanoClock(),
        flywheelUnits,
        () -> flywheelMotor.getCurrentPosition(),
        () -> flywheelMotor.getVelocity(),
        () -> lastFlywheelPower);

MimicSession flywheelMimic = MimicSession.create(flywheelAdapter.observer());
MechanismSnapshot snap = flywheelMimic.observe();
telemetry.addData("flywheel", flywheel.toString());
telemetry.addData("flywheel valid", snap.sensorValid());
```

Repeat the observer pattern for turret and hood. Do not invent travel, hard stops, or PID here. Catalog: [mechanism-kinds.md](../../docs/mechanism-control/mechanism-kinds.md).
