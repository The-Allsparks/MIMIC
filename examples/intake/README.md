# Intake observation sketch (Phase 0)

Generic **intake** family. Typical constructs: roller, spatula, deployable. A claw is an **end effector** composed with an intake, not an intake construct. This is not a named robot intake and not a game-piece type.

Passive observation only. TeamCode still owns `setPower`.

```java
MechanismBlueprint intake = MechanismBlueprint.of("intake", MechanismConstruct.ROLLER_INTAKE);

MechanismUnits units = MechanismUnits.rotaryRadians(
        intake.mechanismId(),
        /* ticksPerRadian */ 1.000,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        intake.mechanismId(),
        new SystemNanoClock(),
        units,
        () -> intakeMotor.getCurrentPosition(),
        () -> intakeMotor.getVelocity(),
        () -> lastCommandedPower);

MimicSession mimic = MimicSession.create(adapter.observer());

MechanismSnapshot snap = mimic.observe();
telemetry.addData("intake", intake.toString());
telemetry.addData("intake valid", snap.sensorValid());
```

Color reject, alliance filters, and spit-versus-divert belong in TeamCode when that hardware exists.
