# Linear mechanism observation sketch (Phase 0)

Generic **lift / elevator** construct. It is **not** BumbleBee hardware and not a promise that Allsparks will build an elevator. Robot names and gear ratios belong in TeamCode. See [library-vs-teamcode.md](../../docs/mechanism-control/library-vs-teamcode.md) and [mechanism-kinds.md](../../docs/mechanism-control/mechanism-kinds.md).

This sketch shows **passive** observation only. It never calls `setPower`.

```java
MechanismBlueprint elevator = MechanismBlueprint.of("elevator", MechanismConstruct.ELEVATOR);

MechanismUnits units = MechanismUnits.linearMillimeters(
        elevator.mechanismId(),
        /* ticksPerMillimeter */ 10.0,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        elevator.mechanismId(),
        new SystemNanoClock(),
        units,
        () -> elevatorMotor.getCurrentPosition(),
        () -> elevatorMotor.getVelocity(),
        () -> lastCommandedPower);

MimicSession mimic = MimicSession.create(adapter.observer());

// inside the OpMode loop, after you command the elevator yourself:
MechanismSnapshot snap = mimic.observe();
telemetry.addData("elev mm", snap.position());
telemetry.addData("elev valid", snap.sensorValid());
```

## Safety for first robot tests

- Adult supervision required.
- Keep the carriage on supports or at a height where a drop cannot injure anyone.
- Maximum initial driver output: keep below a team-documented jog limit (start at a small fraction of full power).
- Stop immediately if motion direction disagrees with the documented positive axis.
- Do not enable Phase 2+ homing or profiled control from this example.
