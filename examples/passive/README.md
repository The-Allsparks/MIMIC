# Passive structure observation sketch (Phase 0)

Generic **passive** family: a funnel, guide, or deployable structure with no piece-path job of its own. It is not an intake, transfer, or driven axis.

Observation only. There is no actuator to command.

```java
MechanismConfiguration declared =
        MechanismConfiguration.builder("funnel")
                .construct(MechanismConstruct.FUNNEL_GUIDE)
                .actuators(ActuatorTopology.none())
                .controlDomain(ControlDomain.PASSIVE_OBSERVATION)
                .calibrationStrategy(CalibrationStrategy.NONE)
                .build();
// Zero actuators is valid here. Do not assume this structure is driven.

MechanismBlueprint funnel = declared.toBlueprint().orElse(
        MechanismBlueprint.of("funnel", MechanismConstruct.FUNNEL_GUIDE));

MechanismUnits units = MechanismUnits.linearMillimeters(
        funnel.mechanismId(),
        /* ticksPerMillimeter */ 1.0,
        DirectionSign.POSITIVE);

MimicSession mimic = MimicSession.create(
        MechanismObserver.builder(funnel.mechanismId(), new SystemNanoClock(), units).build());
MechanismSnapshot snap = mimic.observe();
telemetry.addData("funnel", funnel.toString());
telemetry.addData("funnel valid", snap.sensorValid());
```

If a later season adds a motor to the same physical part, declare a driven family instead of stretching this preset. Do not enable Phase 2+ flags from this example.
