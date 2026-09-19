# Field-element observation sketch (Phase 0)

Generic **field element** family: actuate a field scoring device the robot does not own. Typical constructs: spinner, foundation grabber, beacon pusher, marker deployer. This is not an end-effector gripper for a game piece inside the robot.

Passive observation only. TeamCode still owns `setPower`. Alliance and field-ownership rules belong in TeamCode.

```java
MechanismConfiguration declared =
        MechanismConfiguration.builder("fieldSpinner")
                .construct(MechanismConstruct.CAROUSEL_SPINNER)
                .actuators(ActuatorTopology.singleMotor())
                .controlDomain(ControlDomain.OPEN_LOOP_EFFORT)
                .calibrationStrategy(CalibrationStrategy.NONE)
                .build();
// declared is metadata only. One-shot versus continuous policy is TeamCode.

MechanismBlueprint spinner = declared.toBlueprint().orElse(
        MechanismBlueprint.of("fieldSpinner", MechanismConstruct.CAROUSEL_SPINNER));

MechanismUnits units = MechanismUnits.rotaryRadians(
        spinner.mechanismId(),
        /* ticksPerRadian */ 1.000,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        spinner.mechanismId(),
        new SystemNanoClock(),
        units,
        () -> spinnerMotor.getCurrentPosition(),
        () -> spinnerMotor.getVelocity(),
        () -> lastCommandedPower);

MimicSession mimic = MimicSession.create(adapter.observer());
MechanismSnapshot snap = mimic.observe();
telemetry.addData("field spinner", spinner.toString());
telemetry.addData("field spinner valid", snap.sensorValid());
```

Do not put season device names or alliance colors in MIMIC types. Do not enable Phase 2+ flags from this example.
