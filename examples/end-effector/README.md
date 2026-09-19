# End-effector observation sketch (Phase 0)

Generic **end effector** family: a named-state tool at the end of an arm or lift. Typical constructs: claw, gate, bucket, latch. A claw is not an intake. Intake acquires from the field; the claw grips after that.

Passive observation only. Servo command is not measured pose. TeamCode still owns `setPosition`.

```java
MechanismConfiguration declared =
        MechanismConfiguration.builder("claw")
                .construct(MechanismConstruct.CLAW)
                .actuators(ActuatorTopology.positionalServo())
                .namedStates("open", "close")
                .enable(Capability.NAMED_STATES)
                .calibrationStrategy(CalibrationStrategy.KNOWN_STARTUP_POSE)
                .controlDomain(ControlDomain.NAMED_STATE)
                .build();
// namedStates is a name set. It does not schedule open/close motion.

MechanismBlueprint claw = declared.toBlueprint().orElse(
        MechanismBlueprint.of("claw", MechanismConstruct.CLAW));

MechanismUnits units = MechanismUnits.rotaryRadians(
        claw.mechanismId(),
        /* ticksPerRadian */ 1.000,
        DirectionSign.POSITIVE);

MimicSession mimic = MimicSession.create(
        MechanismObserver.builder(claw.mechanismId(), new SystemNanoClock(), units).build());
MechanismSnapshot snap = mimic.observe();
telemetry.addData("claw", claw.toString());
telemetry.addData("claw valid", snap.sensorValid());
```

Do not treat the last commanded servo position as `RELATIVE_POSITION`. Wire `EXTERNAL_SERVO_FEEDBACK` only when a real sensor exists. Do not enable Phase 6 state engines from this example.
