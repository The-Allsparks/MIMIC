# Climber observation sketch (Phase 0)

Generic **climber** family: support the robot from a hang structure while loaded. Parking in a zone is not this family. Scoring a piece onto a bar is not this family.

Passive observation only. TeamCode still owns `setPower`. A loaded latch must not auto-release.

```java
MechanismConfiguration declared =
        MechanismConfiguration.builder("hang")
                .construct(MechanismConstruct.RATCHET_ASSISTED_HANG)
                .actuators(ActuatorTopology.motorPlusRatchet())
                .sensor("position", SensorRole.RELATIVE_POSITION)
                .sensor("retract", SensorRole.RETRACT_LIMIT)
                .namedStates("stowed", "latched")
                .enable(Capability.HOMING)
                .enable(Capability.SOFT_LIMITS)
                .enable(Capability.NAMED_STATES)
                .calibrationStrategy(CalibrationStrategy.HOME_SWITCH)
                .controlDomain(ControlDomain.NAMED_STATE)
                .build();
// Homing and named states are declared only. This does not winch, home, or release.

MechanismBlueprint hang = declared.toBlueprint().orElse(
        MechanismBlueprint.of("hang", MechanismConstruct.RATCHET_ASSISTED_HANG));

MechanismUnits units = MechanismUnits.linearMillimeters(
        hang.mechanismId(),
        /* ticksPerMillimeter */ 10.0,
        DirectionSign.POSITIVE);

RevMotorObserver adapter = RevMotorObserver.create(
        hang.mechanismId(),
        new SystemNanoClock(),
        units,
        () -> hangMotor.getCurrentPosition(),
        () -> hangMotor.getVelocity(),
        () -> lastCommandedPower);

MimicSession mimic = MimicSession.create(adapter.observer());
MechanismSnapshot snap = mimic.observe();
telemetry.addData("hang mm", snap.position());
telemetry.addData("hang valid", snap.sensorValid());
```

## Safety for first robot tests

- Adult supervision required.
- Supports under the robot so a drop cannot injure anyone.
- Do not home under load.
- Do not auto-release a loaded latch because occupancy is unknown.
- Do not enable Phase 2+ flags from this example. Active control stays behind the safety-gate review.
