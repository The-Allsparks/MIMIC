package org.allsparks.mimic.config;

import java.util.ArrayList;
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
 * robot hardware map.
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
        if (construct == MechanismConstruct.DEPLOYABLE_INTAKE || construct == MechanismConstruct.SPATULA) {
            return namedServo("Deploy or scoop pose is commanded, not measured, unless feedback is wired.");
        }
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("velocity", SensorRole.VELOCITY));
        sensors.add(SensorDeclaration.optional("current", SensorRole.ACTUATOR_CURRENT));
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                EnumSet.noneOf(Capability.class),
                sensors,
                ControlDomain.OPEN_LOOP_EFFORT,
                CalibrationStrategy.NONE,
                LimitPolicy.NONE,
                "Optional velocity and current only. A missing piece sensor is valid.");
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
                    ControlDomain.PASSIVE_OBSERVATION,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "A hopper may be unpowered. Add a motor only when an agitator exists.");
        }
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("current", SensorRole.ACTUATOR_CURRENT));
        EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
        if (construct == MechanismConstruct.FEEDER || construct == MechanismConstruct.COLOR_SORTER) {
            sensors.add(SensorDeclaration.optional("entry", SensorRole.PIECE_ENTRY));
            sensors.add(SensorDeclaration.optional("exit", SensorRole.PIECE_EXIT));
        }
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                capabilities,
                sensors,
                ControlDomain.OPEN_LOOP_EFFORT,
                CalibrationStrategy.NONE,
                LimitPolicy.NONE,
                "Piece sensors are optional. Enable PIECE_COUNTING only when entry/exit evidence exists.");
    }

    private static PresetSuggestion launcher(MechanismConstruct construct) {
        if (construct == MechanismConstruct.FLYWHEEL) {
            List<SensorDeclaration> sensors = new ArrayList<>();
            sensors.add(SensorDeclaration.optional("velocity", SensorRole.VELOCITY));
            return new PresetSuggestion(
                    ActuatorTopology.singleMotor(),
                    EnumSet.of(Capability.READY_AT_SPEED),
                    sensors,
                    ControlDomain.VELOCITY,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "Dual flywheel is the same construct with opposed or independently sensed topology.");
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
                ControlDomain.PROFILED_POSITION,
                CalibrationStrategy.HOME_SWITCH,
                LimitPolicy.SOFT_AND_HARD,
                "Independently sensed sides are a topology choice, not implied by this preset. "
                        + construct.name()
                        + " cascade vs continuous is conversion only.");
    }

    private static PresetSuggestion arm(MechanismConstruct construct) {
        if (construct == MechanismConstruct.HOOD) {
            return namedServo("Hood pose is commanded unless EXTERNAL_SERVO_FEEDBACK is declared.");
        }
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("absolute", SensorRole.ABSOLUTE_POSITION));
        EnumSet<Capability> capabilities =
                EnumSet.of(Capability.SOFT_LIMITS, Capability.HOLD_POSITION, Capability.WRAP_AWARE_ROTATION);
        CalibrationStrategy calibration = CalibrationStrategy.ABSOLUTE_SENSOR;
        if (construct == MechanismConstruct.TURRET) {
            capabilities.add(Capability.HOMING);
        }
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                capabilities,
                sensors,
                ControlDomain.PROFILED_POSITION,
                calibration,
                LimitPolicy.SOFT_ONLY,
                "Absolute sensing is preferred at startup. Compose turret/hood with a launcher; do not treat them as launch energy.");
    }

    private static PresetSuggestion endEffector(MechanismConstruct construct) {
        return namedServo("Named open/close or dump poses. Servo command is not measured position.");
    }

    private static PresetSuggestion climber(MechanismConstruct construct) {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("position", SensorRole.RELATIVE_POSITION));
        sensors.add(SensorDeclaration.optional("retract", SensorRole.RETRACT_LIMIT));
        sensors.add(SensorDeclaration.optional("latch", SensorRole.LATCH_ENGAGED));
        return new PresetSuggestion(
                ActuatorTopology.motorPlusRatchet(),
                EnumSet.of(Capability.HOMING, Capability.SOFT_LIMITS, Capability.NAMED_STATES),
                sensors,
                ControlDomain.NAMED_STATE,
                CalibrationStrategy.HOME_SWITCH,
                LimitPolicy.SOFT_AND_HARD,
                "Loaded latch must not auto-release. Homing under load is a later safety review.");
    }

    private static PresetSuggestion fieldElement(MechanismConstruct construct) {
        if (construct == MechanismConstruct.CAROUSEL_SPINNER) {
            return new PresetSuggestion(
                    ActuatorTopology.singleMotor(),
                    EnumSet.noneOf(Capability.class),
                    new ArrayList<SensorDeclaration>(),
                    ControlDomain.OPEN_LOOP_EFFORT,
                    CalibrationStrategy.NONE,
                    LimitPolicy.NONE,
                    "Field-element spinner. One-shot vs continuous policy belongs in TeamCode.");
        }
        return namedServo("One-shot field-element deployer or grabber.");
    }

    private static PresetSuggestion passive() {
        return new PresetSuggestion(
                ActuatorTopology.none(),
                EnumSet.noneOf(Capability.class),
                new ArrayList<SensorDeclaration>(),
                ControlDomain.PASSIVE_OBSERVATION,
                CalibrationStrategy.NONE,
                LimitPolicy.NONE,
                "No actuator. Observation only.");
    }

    private static PresetSuggestion namedServo(String notes) {
        return new PresetSuggestion(
                ActuatorTopology.positionalServo(),
                EnumSet.of(Capability.NAMED_STATES),
                new ArrayList<SensorDeclaration>(),
                ControlDomain.NAMED_STATE,
                CalibrationStrategy.KNOWN_STARTUP_POSE,
                LimitPolicy.NONE,
                notes);
    }

    private static PresetSuggestion discreteAxis() {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("position", SensorRole.RELATIVE_POSITION));
        sensors.add(SensorDeclaration.optional("index", SensorRole.HOME_INDEX));
        return new PresetSuggestion(
                ActuatorTopology.singleMotor(),
                EnumSet.of(Capability.HOMING, Capability.NAMED_STATES),
                sensors,
                ControlDomain.DISCRETE_INDEX,
                CalibrationStrategy.INDEX_PULSE,
                LimitPolicy.NONE,
                "Advance-one is a later capability. This preset only declares discrete indexing.");
    }

    private static PresetSuggestion storedEnergy() {
        List<SensorDeclaration> sensors = new ArrayList<>();
        sensors.add(SensorDeclaration.optional("cocked", SensorRole.HOME_INDEX));
        sensors.add(SensorDeclaration.optional("release", SensorRole.LATCH_ENGAGED));
        return new PresetSuggestion(
                ActuatorTopology.motorPlusServoRelease(),
                EnumSet.of(Capability.NAMED_STATES, Capability.HOMING),
                sensors,
                ControlDomain.STORED_ENERGY_CYCLE,
                CalibrationStrategy.HOME_SWITCH,
                LimitPolicy.NONE,
                "Stored-energy cycle is declared only. No fire command in Phase 0.");
    }

    public static boolean familyMatches(MechanismConstruct construct) {
        return construct != null && construct.family() != null;
    }

    public static MechanismFamily familyOf(MechanismConstruct construct) {
        return construct.family();
    }
}
