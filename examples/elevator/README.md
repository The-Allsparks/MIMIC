# Elevator observation sketch (Phase 0)

The Allsparks counterbalanced elevator is the first **intended** real target. Hardware selection is incomplete; do not invent motors, sensors, or ratchet wiring. See [elevator-target.md](../../docs/mechanism-control/elevator-target.md).

This sketch shows **passive** observation only. It never calls `setPower`.

```java
MechanismUnits units = MechanismUnits.linearMillimeters(
        "elevator",
        /* ticksPerMillimeter */ 10.0,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        "elevator",
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
