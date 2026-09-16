package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import org.allsparks.mimic.templates.MechanismConstruct;
import org.allsparks.mimic.templates.MechanismFamily;

/**
 * Suggested defaults for {@link MechanismConstruct} presets.
 *
 * Suggestions are not wiring. {@link #exampleConfiguration(String, MechanismConstruct)}
 * exists so desktop tests can prove a preset can be declared valid. It is not a
 * robot hardware map. Optional roles stay off until a caller declares them.
 */
public final class StandardPresets {
    private StandardPresets() {}

    public static PresetSuggestion suggestionFor(MechanismConstruct construct) {
        Objects.requireNonNull(construct, "construct");
        switch (construct.family()) {
            case INTAKE:
                return intake(construct);
            case TRANSFER:
                return transfer(construct);
            case LAUNCHER:
                return launcher(construct);
            case LIFT:
                return lift(construct);
            case ARM:
                return arm(construct);
            case END_EFFECTOR:
                return endEffector(construct);
            case CLIMBER:
                return climber(construct);
            case FIELD_ELEMENT:
                return fieldElement(construct);
            case PASSIVE:
                return passive();
            default:
                return passive();
        }
    }

    /**
     * Desktop example that passes {@link ConfigurationValidator}. Not robot
     * wiring and not an enabled controller.
     *
     * Copies {@link PresetSuggestion#exampleSensors()} and only the capabilities
     * that already have matching evidence on that example. Optional roles are
     * not declared here, so listing them cannot enable a capability.
     */
    public static MechanismConfiguration exampleConfiguration(String mechanismId, MechanismConstruct construct) {
        PresetSuggestion suggestion = suggestionFor(construct);
        MechanismConfiguration.Builder builder =
                MechanismConfiguration.builder(mechanismId)
                        .construct(construct)
                        .actuators(suggestion.topology())
                        .controlDomain(suggestion.controlDomain())
                        .calibrationStrategy(suggestion.calibrationStrategy())
                        .limitPolicy(suggestion.limitPolicy());
        for (Capability capability : suggestion.capabilities()) {
            builder.enable(capability);
        }
        for (SensorDeclaration sensor : suggestion.exampleSensors()) {
            builder.sensor(sensor);
        }
        return builder.build();
    }

    private static PresetSuggestion intake(MechanismConstruct construct) {
        if (construct == MechanismConstruct.DEPLOYABLE_INTAKE) {
            return namedServo(
                    "Deploy pose is commanded, not measured, unless feedback is wired.",
                    "Collision with a lift or other moving assembly.",
                    SensorRole.RETRACT_LIMIT,
                    SensorRole.EXTEND_LIMIT);
        }
        if (construct == MechanismConstruct.SPATULA) {
            return namedServo(
                    "Scoop pose is commanded, not measured, unless feedback is wired.",
                    "Sweep collision.",
                    SensorRole.OBJECT_CONTACT);
        }
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("velocity", SensorRole.VELOCITY));
        sensors.add(SensorDeclaration.optional("current", SensorRole.ACTUATOR_CURRENT));
        if (construct == MechanismConstruct.VACUUM_INTAKE) {
            return new PresetSuggestion(
                    ActuatorTopology.singleMotor(),
                    EnumSet.noneOf(Capability.class),
                    sensors,
                    extras(SensorRole.PIECE_ENTRY),
                    ControlDomain.OPEN_LOOP_EFFORT,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "Optional velocity and current only. A missing piece sensor is valid.",
                    "Illegal pneumatics, loss of piece.");
        }
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                EnumSet.noneOf(Capability.class),
                sensors,
                extras(SensorRole.PIECE_ENTRY, SensorRole.RETRACT_LIMIT, SensorRole.EXTEND_LIMIT),
                ControlDomain.OPEN_LOOP_EFFORT,
                CalibrationStrategy.NONE,
                LimitPolicy.NONE,
                "Optional velocity and current only. A missing piece sensor is valid.",
                "Jam, eject into field, running while stowed.");
    }

    private static PresetSuggestion transfer(MechanismConstruct construct) {
        if (construct == MechanismConstruct.INDEXER || construct == MechanismConstruct.ROTARY_MAGAZINE) {
            return discreteAxis();
        }
        if (construct == MechanismConstruct.HOPPER) {
            return new PresetSuggestion(
                    ActuatorTopology.none(),
                    EnumSet.noneOf(Capability.class),
                    new ArrayList<SensorDeclaration>(),
                    extras(SensorRole.PIECE_ENTRY, SensorRole.PIECE_COUNT),
                    ControlDomain.PASSIVE_OBSERVATION,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "A hopper may be unpowered. Add a motor only when an agitator exists.",
                    "Overflow onto the field.");
        }
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("current", SensorRole.ACTUATOR_CURRENT));
        EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
        List<SensorRole> optional =
                extras(SensorRole.RELATIVE_POSITION, SensorRole.PIECE_ENTRY, SensorRole.PIECE_EXIT);
        String hazardNotes = "Double feed, jam.";
        if (construct == MechanismConstruct.FEEDER) {
            optional = extras(SensorRole.PIECE_ENTRY, SensorRole.PIECE_EXIT);
            hazardNotes = "Fire into an unloaded flywheel.";
        } else if (construct == MechanismConstruct.COLOR_SORTER) {
            optional = extras(SensorRole.PIECE_IDENTITY, SensorRole.PIECE_ENTRY, SensorRole.PIECE_EXIT);
            hazardNotes = "Wrong reject.";
        } else if (construct == MechanismConstruct.PIECE_ACCUMULATOR) {
            optional = extras(SensorRole.PIECE_ENTRY, SensorRole.PIECE_COUNT);
            hazardNotes = "Overflow onto the field.";
        }
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                capabilities,
                sensors,
                optional,
                ControlDomain.OPEN_LOOP_EFFORT,
                CalibrationStrategy.NONE,
                LimitPolicy.NONE,
                "Piece sensors are optional. Enable PIECE_COUNTING only when entry/exit evidence exists.",
                hazardNotes);
    }

    private static PresetSuggestion launcher(MechanismConstruct construct) {
        if (construct == MechanismConstruct.FLYWHEEL) {
            List<SensorDeclaration> sensors = new ArrayList<>();
            sensors.add(SensorDeclaration.optional("velocity", SensorRole.VELOCITY));
            return new PresetSuggestion(
                    ActuatorTopology.singleMotor(),
                    EnumSet.of(Capability.READY_AT_SPEED),
                    sensors,
                    extras(SensorRole.ACTUATOR_CURRENT, SensorRole.PIECE_ENTRY, SensorRole.PIECE_EXIT),
                    ControlDomain.VELOCITY,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "Dual flywheel is the same construct with opposed or independently sensed topology.",
                    "Feeding while slow.");
        }
        return storedEnergy();
    }

    private static PresetSuggestion lift(MechanismConstruct construct) {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("leftPosition", SensorRole.RELATIVE_POSITION));
        sensors.add(SensorDeclaration.optional("home", SensorRole.RETRACT_LIMIT));
        EnumSet<Capability> capabilities =
                EnumSet.of(Capability.HOMING, Capability.SOFT_LIMITS, Capability.HOLD_POSITION);
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                capabilities,
                sensors,
                extras(
                        SensorRole.EXTEND_LIMIT,
                        SensorRole.ABSOLUTE_POSITION,
                        SensorRole.REDUNDANT_POSITION,
                        SensorRole.ACTUATOR_CURRENT),
                ControlDomain.PROFILED_POSITION,
                CalibrationStrategy.HOME_SWITCH,
                LimitPolicy.SOFT_AND_HARD,
                "Independently sensed sides are a topology choice, not implied by this preset. "
                        + construct.name()
                        + " cascade vs continuous is conversion only.",
                "Fall, rack, overrun.");
    }

    private static PresetSuggestion arm(MechanismConstruct construct) {
        if (construct == MechanismConstruct.HOOD) {
            return namedServo(
                    "Hood pose is commanded unless EXTERNAL_SERVO_FEEDBACK is declared.",
                    "Treating servo command as pose.",
                    SensorRole.EXTERNAL_SERVO_FEEDBACK,
                    SensorRole.ABSOLUTE_POSITION);
        }
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("absolute", SensorRole.ABSOLUTE_POSITION));
        EnumSet<Capability> capabilities =
                EnumSet.of(Capability.SOFT_LIMITS, Capability.HOLD_POSITION, Capability.WRAP_AWARE_ROTATION);
        CalibrationStrategy calibration = CalibrationStrategy.ABSOLUTE_SENSOR;
        String hazardNotes = "Cable wrap, gravity drop.";
        if (construct == MechanismConstruct.TURRET) {
            capabilities.add(Capability.HOMING);
            hazardNotes = "Continuous spin versus cable wrap.";
        }
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                capabilities,
                sensors,
                extras(
                        SensorRole.RETRACT_LIMIT,
                        SensorRole.EXTEND_LIMIT,
                        SensorRole.ACTUATOR_CURRENT,
                        SensorRole.ORIENTATION),
                ControlDomain.PROFILED_POSITION,
                calibration,
                LimitPolicy.SOFT_ONLY,
                "Absolute sensing is preferred at startup. Compose turret/hood with a launcher; do not treat them as launch energy.",
                hazardNotes);
    }

    private static PresetSuggestion endEffector(MechanismConstruct construct) {
        if (construct == MechanismConstruct.CLAW) {
            return namedServo(
                    "Named open/close poses. Servo command is not measured position.",
                    "Crush, drop.",
                    SensorRole.OBJECT_CONTACT,
                    SensorRole.EXTERNAL_SERVO_FEEDBACK,
                    SensorRole.LOAD_TENSION);
        }
        return namedServo(
                "Named open/close or dump poses. Servo command is not measured position.",
                "Unconfirmed dump.",
                SensorRole.PIECE_ENTRY,
                SensorRole.LATCH_ENGAGED);
    }

    private static PresetSuggestion climber(MechanismConstruct construct) {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("position", SensorRole.RELATIVE_POSITION));
        sensors.add(SensorDeclaration.optional("retract", SensorRole.RETRACT_LIMIT));
        return new PresetSuggestion(
                ActuatorTopology.motorPlusRatchet(),
                EnumSet.of(Capability.HOMING, Capability.SOFT_LIMITS, Capability.NAMED_STATES),
                sensors,
                extras(SensorRole.LOAD_TENSION, SensorRole.LATCH_ENGAGED, SensorRole.REDUNDANT_POSITION),
                ControlDomain.NAMED_STATE,
                CalibrationStrategy.HOME_SWITCH,
                LimitPolicy.SOFT_AND_HARD,
                "Loaded latch must not auto-release. Homing under load is a later safety review.",
                "Drop from hang.");
    }

    private static PresetSuggestion fieldElement(MechanismConstruct construct) {
        if (construct == MechanismConstruct.CAROUSEL_SPINNER) {
            return new PresetSuggestion(
                    ActuatorTopology.singleMotor(),
                    EnumSet.noneOf(Capability.class),
                    new ArrayList<SensorDeclaration>(),
                    extras(SensorRole.LATCH_ENGAGED),
                    ControlDomain.OPEN_LOOP_EFFORT,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "Field-element spinner. One-shot vs continuous policy belongs in TeamCode.",
                    "Spinning an alliance field device illegally.");
        }
        return namedServo(
                "One-shot field-element deployer or grabber.",
                "Leaving a latch on the field.",
                SensorRole.LATCH_ENGAGED);
    }

    private static PresetSuggestion passive() {
        return new PresetSuggestion(
                ActuatorTopology.none(),
                EnumSet.noneOf(Capability.class),
                new ArrayList<SensorDeclaration>(),
                extras(SensorRole.LATCH_ENGAGED),
                ControlDomain.PASSIVE_OBSERVATION,
                CalibrationStrategy.NONE,
                LimitPolicy.NONE,
                "No actuator. Observation only.",
                "Assuming it is driven.");
    }

    private static PresetSuggestion namedServo(String notes, String hazardNotes, SensorRole... optionalRoles) {
        return new PresetSuggestion(
                ActuatorTopology.positionalServo(),
                EnumSet.of(Capability.NAMED_STATES),
                new ArrayList<SensorDeclaration>(),
                extras(optionalRoles),
                ControlDomain.NAMED_STATE,
                CalibrationStrategy.KNOWN_STARTUP_POSE,
                LimitPolicy.NONE,
                notes,
                hazardNotes);
    }

    private static PresetSuggestion discreteAxis() {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("position", SensorRole.RELATIVE_POSITION));
        sensors.add(SensorDeclaration.optional("index", SensorRole.HOME_INDEX));
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                EnumSet.of(Capability.HOMING, Capability.NAMED_STATES),
                sensors,
                extras(SensorRole.PIECE_ENTRY, SensorRole.PIECE_EXIT, SensorRole.PIECE_COUNT),
                ControlDomain.DISCRETE_INDEX,
                CalibrationStrategy.INDEX_PULSE,
                LimitPolicy.NONE,
                "Advance-one is a later capability. This preset only declares discrete indexing.",
                "Skip pocket, crush piece.");
    }

    private static PresetSuggestion storedEnergy() {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("cocked", SensorRole.HOME_INDEX));
        sensors.add(SensorDeclaration.optional("release", SensorRole.LATCH_ENGAGED));
        return new PresetSuggestion(
                ActuatorTopology.motorPlusServoRelease(),
                EnumSet.of(Capability.NAMED_STATES, Capability.HOMING),
                sensors,
                extras(SensorRole.ACTUATOR_CURRENT, SensorRole.RELATIVE_POSITION),
                ControlDomain.STORED_ENERGY_CYCLE,
                CalibrationStrategy.HOME_SWITCH,
                LimitPolicy.NONE,
                "Stored-energy cycle is declared only. No fire command in Phase 0.",
                "Stored energy.");
    }

    private static List<SensorRole> extras(SensorRole... roles) {
        return Arrays.asList(roles);
    }

    public static boolean familyMatches(MechanismConstruct construct) {
        return construct != null && construct.family() != null;
    }

    public static MechanismFamily familyOf(MechanismConstruct construct) {
        return construct.family();
    }
}
