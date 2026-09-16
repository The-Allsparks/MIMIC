# Transfer observation sketch (Phase 0)

**Transfer** is the internal path that moves a piece through the robot. Prefer this word over "shoot". Typical constructs: belt, roller path, indexer, feeder.

Passive observation only. TeamCode still owns `setPower`.

```java
MechanismBlueprint feeder = MechanismBlueprint.of("feeder", MechanismConstruct.FEEDER);

MechanismUnits units = MechanismUnits.rotaryRadians(
        feeder.mechanismId(),
        /* ticksPerRadian */ 1.000,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        feeder.mechanismId(),
        new SystemNanoClock(),
        units,
        () -> feederMotor.getCurrentPosition(),
        () -> feederMotor.getVelocity(),
        () -> lastCommandedPower);

MimicSession mimic = MimicSession.create(adapter.observer());

MechanismSnapshot snap = mimic.observe();
telemetry.addData("feeder", feeder.toString());
telemetry.addData("feeder valid", snap.sensorValid());
```

Jam logic against an intake reject sensor is a named TeamCode interlock later, not a MIMIC type.
