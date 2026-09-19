package org.allsparks.mimic.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Suggested defaults for a standard construct. Suggestions are not wiring and
 * do not enable a capability on a robot.
 *
 * {@link #exampleSensors()} is the smallest declared set that can make a valid
 * desktop example when paired with {@link #capabilities()}.
 * {@link #optionalRoles()} are extras a team can add or delete; they are not
 * HardwareMap names and are not copied into example configurations.
 */
public final class PresetSuggestion {
    private final ActuatorTopology topology;
    private final EnumSet<Capability> capabilities;
    private final List<SensorDeclaration> exampleSensors;
    private final List<SensorRole> optionalRoles;
    private final ControlDomain controlDomain;
    private final CalibrationStrategy calibrationStrategy;
    private final LimitPolicy limitPolicy;
    private final String notes;
    private final String hazardNotes;

    public PresetSuggestion(
            ActuatorTopology topology,
            Set<Capability> capabilities,
            List<SensorDeclaration> exampleSensors,
            List<SensorRole> optionalRoles,
            ControlDomain controlDomain,
            CalibrationStrategy calibrationStrategy,
            LimitPolicy limitPolicy,
            String notes,
            String hazardNotes) {
        this.topology = topology == null ? ActuatorTopology.none() : topology;
        this.capabilities =
                capabilities == null || capabilities.isEmpty()
                        ? EnumSet.noneOf(Capability.class)
                        : EnumSet.copyOf(capabilities);
        this.exampleSensors =
                exampleSensors == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(new ArrayList<>(exampleSensors));
        this.optionalRoles = copyOptionalExtras(this.exampleSensors, optionalRoles);
        this.controlDomain = controlDomain == null ? ControlDomain.PASSIVE_OBSERVATION : controlDomain;
        this.calibrationStrategy =
                calibrationStrategy == null ? CalibrationStrategy.NONE : calibrationStrategy;
        this.limitPolicy = limitPolicy == null ? LimitPolicy.NONE : limitPolicy;
        this.notes = notes == null ? "" : notes;
        this.hazardNotes = hazardNotes == null ? "" : hazardNotes;
    }

    private static List<SensorRole> copyOptionalExtras(
            List<SensorDeclaration> exampleSensors, List<SensorRole> optionalRoles) {
        EnumSet<SensorRole> exampleRoles = EnumSet.noneOf(SensorRole.class);
        for (SensorDeclaration sensor : exampleSensors) {
            exampleRoles.add(sensor.role());
        }
        List<SensorRole> extras = new ArrayList<>();
        if (optionalRoles == null) {
            return Collections.emptyList();
        }
        for (SensorRole role : optionalRoles) {
            if (role == null || exampleRoles.contains(role) || extras.contains(role)) {
                continue;
            }
            extras.add(role);
        }
        return Collections.unmodifiableList(extras);
    }

    public ActuatorTopology topology() {
        return topology;
    }

    public Set<Capability> capabilities() {
        return Collections.unmodifiableSet(capabilities);
    }

    /**
     * Example sensor declarations that make a valid configuration for tests and
     * docs. They are not a claim that the robot has those sensors.
     */
    public List<SensorDeclaration> exampleSensors() {
        return exampleSensors;
    }

    /**
     * Extra catalog roles a team can add or delete. These are not wired, not
     * HardwareMap names, and not included in
     * {@link StandardPresets#exampleConfiguration}.
     */
    public List<SensorRole> optionalRoles() {
        return optionalRoles;
    }

    public ControlDomain controlDomain() {
        return controlDomain;
    }

    public CalibrationStrategy calibrationStrategy() {
        return calibrationStrategy;
    }

    public LimitPolicy limitPolicy() {
        return limitPolicy;
    }

    public String notes() {
        return notes;
    }

    /**
     * Catalog hazard phrases for this construct. Not a controller and not a
     * claim that those faults are implemented.
     */
    public String hazardNotes() {
        return hazardNotes;
    }
}
