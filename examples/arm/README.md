# Arm observation sketch (Phase 0)

Generic **arm** family: rotary positioning. Typical constructs: pivot, wrist, turret, hood. Turret and hood are aim axes. They are not launch energy. Compose them with a [launcher](../launcher/README.md) using separate ids and observers.

Passive observation only. TeamCode still owns `setPower`.

```java
MechanismConfiguration declared =
        MechanismConfiguration.builder("wrist")
                .construct(MechanismConstruct.WRIST)
                .actuators(ActuatorTopology.singleMotor())
                .sensor("absolute", SensorRole.ABSOLUTE_POSITION)
                .enable(Capability.SOFT_LIMITS)
                .enable(Capability.HOLD_POSITION)
                .calibrationStrategy(CalibrationStrategy.ABSOLUTE_SENSOR)
                .controlDomain(ControlDomain.PROFILED_POSITION)
                .build();
// declared is metadata only. It does not home, hold, or write a servo.

MechanismBlueprint wrist = declared.toBlueprint().orElse(
        MechanismBlueprint.of("wrist", MechanismConstruct.WRIST));

MechanismUnits units = MechanismUnits.rotaryRadians(
        wrist.mechanismId(),
        /* ticksPerRadian */ 1.000,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        wrist.mechanismId(),
        new SystemNanoClock(),
        units,
        () -> wristMotor.getCurrentPosition(),
        () -> wristMotor.getVelocity(),
        () -> lastCommandedPower);

MimicSession mimic = MimicSession.create(adapter.observer());
MechanismSnapshot snap = mimic.observe();
telemetry.addData("wrist rad", snap.position());
telemetry.addData("wrist valid", snap.sensorValid());
```

Do not invent travel, hard stops, or PID here. Do not enable Phase 2+ homing or profiled control from this example. Catalog: [mechanism-kinds.md](../../docs/mechanism-control/mechanism-kinds.md).
